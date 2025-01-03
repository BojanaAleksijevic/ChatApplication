package rs.raf.pds.v4.z5.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryonet.Connection;

import rs.raf.pds.v4.z5.messages.ChatMessage;

public class Room {
	private String roomName;
	private final Map<String, Connection> users; // Mapiranje korisničkog imena na konekciju
	private final List<String> allMessages = new ArrayList<>();
	private final LinkedList<String> messageHistory; // Čuva poslednjih 10 poruka

    public Room() {
		this.users = null;
		this.messageHistory = null;
    }
    
    public Room(String roomName) {
        this.roomName = roomName;
        this.users = new HashMap<>();
        this.messageHistory = new LinkedList<>();
    }
	
    public Room(String roomName, List<String> userNames, List<String> messages) {
        this.roomName = roomName;
        this.users = new ConcurrentHashMap<>();
        for (String userName : userNames) {
            this.users.put(userName, null); // Konekcija se može naknadno dodeliti
        }
        this.messageHistory = new LinkedList<>();
        if (messages != null) {
            for (String message : messages) {
                addMessage(message); // Dodavanje postojećih poruka uz poštovanje limita
            }
        }
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
	
	public void addMessage(String message) {
		allMessages.add(message);
		
		// poslednjih 10 poruka
	    messageHistory.add(message);
	    if (messageHistory.size() > 10) {
	        messageHistory.remove(0); // Ukloni najstariju poruku
	    }
    }
    

	public List<String> getMessageHistory() {
        return List.copyOf(messageHistory);
    }
	
	public List<String> getAllMessages() {
	    return List.copyOf(allMessages);
	}

    
    
	public void broadcast(String message, Connection exception) {
        for (Connection conn : users.values()) {
            if (conn != exception && conn != null && conn.isConnected()) {
                conn.sendTCP(new ChatMessage("Room " + roomName, message));
            }
        }
    }
    
    @Override
    public String toString() {
        return roomName;
    }

	
}
