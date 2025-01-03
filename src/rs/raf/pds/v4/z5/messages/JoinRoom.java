package rs.raf.pds.v4.z5.messages;

public class JoinRoom {
	private String roomName;
	private String userName;
	
	public JoinRoom() {
		
	}
	
	public JoinRoom(String roomName,String userName) {
		this.roomName = roomName;
		this.userName = userName;
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public String getUserName() {
		return userName;
	}
}
