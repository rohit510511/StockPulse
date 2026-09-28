package StockPlus.demo.dto;

import StockPlus.demo.model.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public class SuggestionResolutionRequest {

    @NotNull(message = "Status is required")
    private SuggestionStatus status;

    public SuggestionResolutionRequest() {
    }

    public SuggestionResolutionRequest(SuggestionStatus status) {
        this.status = status;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }
}
