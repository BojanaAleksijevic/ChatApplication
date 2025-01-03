package rs.raf.pds.v4.z5.messages;

public class RoomMessage {
	private String user;
	private String roomName;
	private String txt;
	
	protected RoomMessage() {
		
	}
	
	public RoomMessage(String roomName, String txt) {
		this.roomName = roomName;
		this.txt = txt;
	}
	
	
	public RoomMessage(String user, String roomName, String txt) {
		this.user = user;
		this.roomName = roomName;
		this.txt = txt;
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
	
}
