package StockPlus.demo.advisor;

public class CategoryStats {

    private final double averageDemandVelocity;
    private final long productCount;

    public CategoryStats(double averageDemandVelocity, long productCount) {
        this.averageDemandVelocity = averageDemandVelocity;
        this.productCount = productCount;
    }

    public double getAverageDemandVelocity() {
        return averageDemandVelocity;
    }

    public long getProductCount() {
        return productCount;
    }
}
