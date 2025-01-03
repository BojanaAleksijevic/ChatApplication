package rs.raf.pds.v4.z5.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryonet.Connection;

import rs.raf.pds.v4.z5.messages.ChatMessage;

public class Room {
	private String roomName;
	private final Map<String, Connection> users; // Mapiranje korisničkog imena na konekciju
    private final List<String> messages;

    public Room() {
		this.users = null;
		this.messages = null;
    }
    
    public Room(String roomName) {
        this.roomName = roomName;
        this.users = new HashMap<>();
        this.messages = new ArrayList<>();
    }
	
	public Room(String roomName, List<String> userNames, List<String> messages) {
	    this.roomName = roomName;
	    this.users = new ConcurrentHashMap<>();
	    for (String userName : userNames) {
	        this.users.put(userName, null);
	    }
	    this.messages = messages != null ? messages : new ArrayList<>();
	}


	public String getRoomName() {
		return roomName;
	}
	
	public String getRoomName(Room room) {
	    return room.getRoomName();
	}

	
	public void addUser(String userName, Connection connection) {
	    users.put(userName, connection);
	}

	
	public void removeUser(String userName) {
		users.remove(userName);
	}
	
	public List<String> getUsers() {
	    return new ArrayList<>(users.keySet());
	}

	
	public boolean isEmpty() {
        return users.isEmpty();
    }
	
    /*
    public void addMessage(String message) {
    	// Dodaje novu poruku u listu poruka.
    	if (messages.size() >= 10) {
    		messages.remove(0);
    	}
    	messages.add(message);
    }*/
	
	public void addMessage(String message) {
        this.messages.add(message);
    }
    
	public List<String> getMessages() {
	    return new ArrayList<>(messages);
	}

    
    
    public void broadcast(String message, Connection exception) {
        for (Connection conn : users.values()) {
            if (conn != exception && conn.isConnected()) {
                conn.sendTCP(new ChatMessage("Room " + roomName, message));
            }
        }
    }
    
    @Override
    public String toString() {
        return roomName;
    }

	
}
