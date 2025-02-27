package rs.raf.pds.v4.z5.messages;

public class RoomMessage implements Message{
	private int id;
	protected String user;
	protected String roomName;
	private String txt;
	
	private int replyToMessageId;  // ID poruke kojoj se odgovara
    private String replyToText;    // Tekst poruke kojoj se odgovara
	private boolean isRoomMessage;
    
	protected RoomMessage() {
		
	}
	
	public RoomMessage(String roomName, String txt) {
		this.roomName = roomName;
		this.txt = txt;
	}
	
	
	
	public RoomMessage(String user, String roomName, String txt) {
        this(user, roomName, txt, 0, ""); // Ako nema odgovora
    }
	
	
	public RoomMessage(String user, String roomName, String txt, Integer replyToMessageId, String replyToText) {
        this.user = user;
        this.roomName = roomName;
        this.txt = txt;
        this.replyToMessageId = replyToMessageId;
        this.replyToText = replyToText;
    }
	
	public RoomMessage(String user, String roomName, String txt, boolean isRoomMessage) {
	    this.user = user;
	    this.roomName = roomName;
	    this.txt = txt;
	    this.isRoomMessage = isRoomMessage;
	    
	    System.out.println(">>> Napravljena RoomMessage: " + txt + " (isRoomMessage=" + isRoomMessage + ")");
	}

	public RoomMessage(String user, String roomName, String txt, boolean isRoomMessage, int id) {
	    this.user = user;
	    this.roomName = roomName;
	    this.txt = txt;
	    this.isRoomMessage = isRoomMessage;
	    this.id = id;  // Postavljanje ID-a
	}

	
	@Override
    public int getId() {
        return id;
    }
	
	@Override
    public void setId(int id) {
        this.id = id;
    }

	public String getUser() {
		return user;
	}

	public String getRoomName() {
        return roomName;
    }
	

	public String getTxt() {
		return txt;
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
    
    public boolean isRoomMessage() {
        return isRoomMessage;
    }
	
}
