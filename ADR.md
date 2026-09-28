# StockPulse — Architectural Decision Records (ADR)

This document outlines the core architectural and design decisions adopted in **StockPulse — AI Inventory & Dynamic Pricing Engine**.

---

## 1. Where Commerce Logic Lives

### Context
ShopStream requires dynamic inventory evaluation and pricing recommendations. We needed to determine where domain reasoning should reside to maintain clear separation of concerns, testability, and clean architecture.

### Options
1. **Controller Layer**: Embed pricing and replenishment logic directly within HTTP endpoints.
2. **Entity / Domain Model**: Place dynamic advisory rules directly inside `Product` JPA entity methods.
3. **Pluggable Advisor Strategy (`advisor/`)**: Isolate commerce rules behind a unified `CommerceAdvisor` interface coordinated by a domain `RecommendationService`.

### Decision
**Adopt Option 3: Pluggable Advisor Strategy (`StockPlus.demo.advisor.CommerceAdvisor`)**.
Controllers only handle request validation, status translation, and HTTP dispatching. The entity maintains data invariants and status state transitions. All dynamic pricing calculations, threshold analysis, and AI reasoning are encapsulated behind the `CommerceAdvisor` interface.

### Tradeoffs
- **Pros**: Controllers and domain services remain completely decoupled from specific advisory algorithms. Enables clean mocking in unit tests and dynamic runtime strategy substitution without modifying application endpoints.
- **Cons**: Slightly more initial boilerplate (interfaces, DTOs, value objects).

---

## 2. Unified vs. Separate AI Calls

### Context
Every inventory signal evaluation potentially requires both a pricing adjustment and an inbound reorder recommendation. We considered whether to call the LLM once for both recommendations or execute two separate LLM calls.

### Options
1. **Separate LLM Calls**: One prompt for pricing and a second independent prompt for replenishment reorder.
2. **Unified LLM Call**: A single prompt presenting all product and category signals, requesting a single structured JSON response containing both `pricing` and `reorder` objects.

### Decision
**Adopt Option 2: Unified LLM Call (`AICommerceStrategy`)**.
A single chat completion request (`qwen-cursor`) is dispatched with structured guidance for both pricing and replenishment.

### Tradeoffs
- **Pros**:
  - **Coherence**: Pricing and reorder strategies directly affect one another (e.g., if reorder lead time is long and stock is critically low, the price should increase more sharply to throttle velocity until stock arrives). A single context allows the LLM to harmonize the decisions.
  - **Latency & Quota Efficiency**: Reduces round-trip network latency from $2 \times 1.5\text{s} = 3.0\text{s}$ down to a single call (~1.5s), halving API token overhead and rate-limit consumption.
- **Cons**: Prompt schema complexity is higher, requiring defensive parsing for two composite objects within the response.

---

## 3. Dynamic Runtime Strategy Switching

### Context
Merchandisers and system operators must be able to switch between the deterministic `RuleBasedStrategy` and `AICommerceStrategy` at runtime without server restarts or redeployments.

### Options
1. **Spring Profiles / Configuration Reload**: Require application restart with `--spring.profiles.active=ai` or Spring Cloud Config refresh.
2. **Dynamic Strategy Registry Holder (`advisor.StrategyRegistry`)**: Maintain an `AtomicReference<StrategyType>` in memory, exposed via `GET/PUT /admin/strategy`.

### Decision
**Adopt Option 2: Dynamic Strategy Registry Holder**.
The `StrategyRegistry` bean holds the active strategy type (initialized from `commerce.strategy.default=AI` in `application.properties`) and provides lock-free, zero-restart runtime switching through `PUT /admin/strategy`.

### Tradeoffs
- **Pros**: Instantaneous strategy switching with zero downtime; non-disruptive live demonstrations; trivial to inspect via HTTP.
- **Cons**: In-memory state is local to the instance (in a clustered multi-node deployment, this would be backed by Redis or distributed configuration).

---

## 4. LLM Failure Handling & Graceful Degradation

### Context
Remote LLMs are susceptible to transient network partitions, timeouts, rate limits (HTTP 429), missing API keys, or schema hallucinations. The system requirement states: *"A failure must never silently drop a recommendation."*

### Options
1. **Fail-Fast & Return Error**: Throw 500/502 errors back to the caller when LLM fails.
2. **Silent Retry Loops**: Block execution and retry the LLM indefinitely.
3. **Defensive Parsing, Sane Bounding & Rule Fallback**: Catch all exceptions, parse defensively, validate sanity bounds (50%–200% price ratio, positive quantities), and immediately fallback to `RuleBasedStrategy`, tagging `source = RULE_FALLBACK`.

### Decision
**Adopt Option 3: Defensive Parsing, Sane Bounding & Rule Fallback**.
`AICommerceStrategy` wraps the remote client invocation with defensive markdown stripping (handles ````json code fences and surrounding text), validates that proposed prices fall within 50%–200% of current price, and upon *any* parse error, validation failure, timeout, or HTTP error, immediately delegates to `RuleBasedStrategy`, marking suggestions with `source = RULE_FALLBACK` and appending the failure rationale.

### Tradeoffs
- **Pros**: Zero dropped recommendations; zero 500 error cascades; merchandisers always receive actionable recommendations with clear transparency on whether rules or AI generated them.
- **Cons**: Requires keeping rule-based logic continuously aligned as a reliable safety net.

---

## 5. Event-Driven Agentic Loop Decoupling

### Context
Inventory signals occur when users place orders (`POST /products/{id}/orders`) or update stock (`PATCH /products/{id}/stock`). High-frequency checkout paths must not be blocked by 5–15 second LLM inferences.

### Options
1. **Synchronous In-Line Processing**: Run the AI advisory loop directly inside the checkout request thread.
2. **Decoupled Asynchronous Event Loop (`@Async @TransactionalEventListener(AFTER_COMMIT)`)**: Publish a domain event upon checkout; process triggers in a bounded background thread pool only after the database transaction has committed.

### Decision
**Adopt Option 2: Decoupled Asynchronous Event Loop**.
`ProductService` publishes `ProductInventoryChangedEvent`. The `StockPulseEventListener` processes the event asynchronously using a dedicated bounded `ThreadPoolTaskExecutor`. It evaluates Trigger A (`INVENTORY_LOW`) and Trigger B (`DEMAND_SPIKE`) in the background.

### Tradeoffs
- **Pros**: Checkout and stock modification endpoints return in $< 10\text{ms}$. If LLM takes several seconds or fails, the user's order is already safely committed.
- **Cons**: Eventual consistency — suggestions appear a few moments after checkout rather than in the immediate HTTP response body.

---

## 6. Extensibility and Deliberate Scope Omissions

### Context
StockPulse is designed as a focused, high-impact reactive commerce engine. To ensure architectural purity and maintainability within the evaluation scope, certain peripheral features were deliberately omitted.

### Deliberate Omissions:
1. **Automated Purchase Orders (POs)**: StockPulse creates suggestions for human approval. It deliberately *never* automatically sends purchase orders or transfers money.
2. **Storefront & Payment Gateways**: Not an e-commerce customer storefront; focused entirely on the merchant's advisory and pricing control plane.
3. **Competitor Price Web Scraping**: External website scraping introduces brittle DOM dependencies and bot-block issues; replaced with robust category-wide benchmark statistics.
4. **Authentication & Authorization**: Omitted to keep the API surface accessible for automated hackathon evaluation scripts.

### Extensibility Hook Points:
- `Product.costPrice` and `Product.supplierId` fields are already present in the domain model, allowing seamless Sprint 2 integration of supplier catalog APIs and margin protection algorithms.
- `CommerceAdvisor` is an open interface enabling future reinforcement learning or multi-agent debate strategies.
