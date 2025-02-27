package rs.raf.pds.v4.z5.messages;

public class ReplyMessage extends RoomMessage {
    private int replyToMessageId;  // ID poruke kojoj se odgovara
    private String replyToText;    // Tekst poruke kojoj se odgovara

    public ReplyMessage() {
    	super();
    }
    
    public ReplyMessage(String user, String roomName, String txt, int replyToMessageId, String replyToText) {
        super(user, roomName, txt); // Pozivanje konstruktora roditeljske klase RoomMessage
        this.replyToMessageId = replyToMessageId;
        this.replyToText = replyToText;
    }
    /*
    public ReplyMessage(String roomName, String txt, int replyToMessageId, String replyToText) {
        super(roomName, txt); // Pozivanje konstruktora roditeljske klase RoomMessage
        this.replyToMessageId = replyToMessageId;
        this.replyToText = replyToText;
    }*/
    
    public ReplyMessage(String user, String txt, int replyToMessageId, String replyToText) {
        super(user, "", txt); // Drugi parametar null jer roomName ovde nije dostupan na klijentu
        this.replyToMessageId = replyToMessageId;
        this.replyToText = replyToText;
    }



    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public String getReplyToText() {
        return replyToText;
    }
    
    public void setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }
}
