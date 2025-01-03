package rs.raf.pds.v4.z5.messages;

public class InviteUser {
	private String roomName;
	private String invitedUser;
	
	public InviteUser() {
    }
	
	public InviteUser(String roomName, String invitedUser) {
		this.roomName = roomName;
		this.invitedUser = invitedUser;
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public String getInvitedUser() {
		return invitedUser;
	}
}
