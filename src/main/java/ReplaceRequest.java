
public class ReplaceRequest {

    private long origOrderId;
    private int newSize;

    public ReplaceRequest(long origOrderId, int newSize) {
        this.origOrderId = origOrderId;
        this.newSize = newSize;
    }

    public long getOrigOrderId() {
        return origOrderId;
    }

    public void setOrigOrderId(long origOrderId) {
        this.origOrderId = origOrderId;
    }

    public int getNewSize() {
        return newSize;
    }

    public void setNewSize(int newSize) {
        this.newSize = newSize;
    }
}
