public class Order {

    private long orderId;
    private double price;
    private char side;
    private long size;

    private OrdStatus ordStatus; // FIX tag 9
    private long cumQty;
    private long leavesQty;
    private double lastPx; // FIX tag 31

    public Order(long orderId, double price, char side, long size) {
        this.orderId = orderId;
        this.price = price;
        this.side = side;
        this.size = size;

        this.cumQty = 0;
        this.leavesQty = size;
    }


    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public char getSide() {
        return side;
    }

    public void setSide(char side) {
        this.side = side;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public OrdStatus getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(OrdStatus ordStatus) {
        this.ordStatus = ordStatus;
    }

    public long getCumQty() {
        return cumQty;
    }

    public void setCumQty(long cumQty) {
        this.cumQty = cumQty;
    }

    public long getLeavesQty() {
        return leavesQty;
    }

    public void setLeavesQty(long leavesQty) {
        this.leavesQty = leavesQty;
    }

    public double getLastPx() {
        return lastPx;
    }

    public void setLastPx(double lastPx) {
        this.lastPx = lastPx;
    }
}
