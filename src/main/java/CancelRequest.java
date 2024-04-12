
public class CancelRequest {

    private long origOrderId;

    public CancelRequest(long origOrderId) {
        this.origOrderId = origOrderId;
    }

    public long getOrigOrderId() {
        return origOrderId;
    }

    public void setOrigOrderId(long origOrderId) {
        this.origOrderId = origOrderId;
    }
}
