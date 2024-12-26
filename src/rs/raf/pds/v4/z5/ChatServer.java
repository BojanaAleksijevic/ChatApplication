package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.WhoRequest;

public class ChatServer implements Runnable {

	private volatile Thread thread = null;

	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();

	public ChatServer(int portNumber) {
		this.server = new Server();

		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}

	private void registerListener() {
		server.addListener(new Listener() {
			public void received(Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login) object;
					newUserLogged(login, connection);
					connection.sendTCP(new InfoMessage("Hello " + login.getUserName()));
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}

				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage) object;
					System.out.println(chatMessage.getUser() + ": " + chatMessage.getTxt());
					broadcastChatMessage(chatMessage, connection);
					return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
			}

			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user + " has disconnected!", connection);
			}
		});
	}

	String[] getAllUsers() {
		String[] users = new String[userConnectionMap.size()];
		int i = 0;
		for (String user : userConnectionMap.keySet()) {
			users[i] = user;
			i++;
		}

		return users;
	}

	void newUserLogged(Login loginMessage, Connection conn) {
		userConnectionMap.put(loginMessage.getUserName(), conn);
		connectionUserMap.put(conn, loginMessage.getUserName());
		showTextToAll("User " + loginMessage.getUserName() + " has connected!", conn);
	}

	private void broadcastChatMessage(ChatMessage message, Connection exception) {
		String text = message.getTxt();

		// proverava da li je poruka privatna
		if (text.contains(":")) {
			String[] parts = text.split(":", 2);
			String recipientsPart = parts[0].trim();
			String privateMessage = parts[1].trim();

			if (recipientsPart.contains(",")) {
				// multicast
				String[] recipients = recipientsPart.split(",");
				for (String recipientName : recipients) {
					recipientName = recipientName.trim(); // bez razmaka
					Connection recipientConnection = userConnectionMap.get(recipientName);

					if (recipientConnection != null && recipientConnection.isConnected()) {
						recipientConnection.sendTCP(new ChatMessage(message.getUser(), "(Group) " + privateMessage));
					} else {
						exception.sendTCP(new InfoMessage("User " + recipientName + " is not available."));
					}
				}

			} else {
				// privatne poruke

				Connection recipientConnection = userConnectionMap.get(recipientsPart);
				if (recipientConnection != null && recipientConnection.isConnected()) {
					// salje privatnu poruku samo primaocu
					recipientConnection.sendTCP(new ChatMessage(message.getUser(), "(Private) " + privateMessage));
					// obavesti posiljaoca (exception) da je poruka poslata
					exception.sendTCP(new InfoMessage("Private message sent to user: " + recipientsPart));
				} else {
					// obavesti posiljaoca da korisnik ne postoji
					exception.sendTCP(new InfoMessage("User " + recipientsPart + " is not available."));
				}
			}
		} else {
			// u suprotnom, prosledi poruku svima (broadcast)
			for (Connection conn : userConnectionMap.values()) {
				if (conn.isConnected() && conn != exception) {
					conn.sendTCP(message);
				}
			}
		}
	}

	private void showTextToAll(String txt, Connection exception) {
		System.out.println(txt);
		for (Connection conn : userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(new InfoMessage(txt));
		}
	}

	public void start() throws IOException {
		server.start();
		server.bind(portNumber);

		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}

	@Override
	public void run() {
		running = true;

		while (running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: java -jar chatServer.jar <port number>");
			System.out.println("Recommended port number is 54555");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);
		try {
			ChatServer chatServer = new ChatServer(portNumber);
			chatServer.start();

			chatServer.thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
