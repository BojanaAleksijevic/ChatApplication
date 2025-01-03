package rs.raf.pds.v4.z5.messages;

import java.util.List;

public class ListRooms {
	private List<String> roomNames;
	
	public ListRooms() {
		
	}
	
	public ListRooms(List<String> roomNames) {
		this.roomNames = roomNames;
	}
	
	public List<String> getRoomNames() {
		return roomNames;
	}
}
