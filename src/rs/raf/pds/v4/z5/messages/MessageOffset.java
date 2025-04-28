package rs.raf.pds.v4.z5.messages;

public class MessageOffset {
    private int startOffset;
    private int endOffset;
    private RoomMessage message;

    public MessageOffset(int startOffset, int endOffset, RoomMessage message) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.message = message;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public RoomMessage getMessage() {
        return message;
    }

    public void setMessage(RoomMessage message) {
        this.message = message;
    }
}
