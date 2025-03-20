package rs.raf.pds.v4.z5;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.Style;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.CreateRoom;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.JoinRoom;

public class ChatClientGUI {
    private ChatClient chatClient;
    private JFrame frame;
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private String username;
    private JTextArea joinedRoomsArea;

    private List<String> availableRooms = new ArrayList<>();
    private List<String> joinedRooms = new ArrayList<>();
    private boolean awaitingRoomList = false;

    public ChatClientGUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.username = chatClient.userName;

        frame = new JFrame("Chat App - " + username);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top panel koji sadrži dugmiće i spisak soba
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createRoomButton = new JButton("Create Room");
        JButton joinRoomButton = new JButton("Join Room");
        JButton inviteUserButton = new JButton("Invite User");
        buttonPanel.add(createRoomButton);
        buttonPanel.add(joinRoomButton);
        buttonPanel.add(inviteUserButton);

        JPanel roomsPanel = new JPanel(new BorderLayout());
        JLabel roomsLabel = new JLabel(" Rooms you are a member of:");
        joinedRoomsArea = new JTextArea(3, 50);
        joinedRoomsArea.setEditable(false);
        joinedRoomsArea.setLineWrap(true);
        joinedRoomsArea.setWrapStyleWord(true);
        JScrollPane roomsScroll = new JScrollPane(joinedRoomsArea);
        roomsPanel.add(roomsLabel, BorderLayout.NORTH);
        roomsPanel.add(roomsScroll, BorderLayout.CENTER);

        topPanel.add(buttonPanel);
        topPanel.add(roomsPanel);
        frame.add(topPanel, BorderLayout.NORTH);

        // Chat prikaz
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        frame.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // Polje za unos poruka
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Listener za dolazne poruke
        chatClient.setOnMessageReceivedListener(this::displayMessage);

        // Slanje poruka na dugme i enter
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Dugme za kreiranje sobe
        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(frame, "Enter room name:");
            if (roomName != null && !roomName.trim().isEmpty()) {
                chatClient.sendObject(new CreateRoom(roomName.trim(), username));
            }
        });

        // Dugme za join room
        joinRoomButton.addActionListener(e -> {
            awaitingRoomList = true;
            chatClient.sendObject(new InfoMessage("GET_ROOMS"));
        });

        frame.setVisible(true);
        
        
        // dugme za invite user
        inviteUserButton.addActionListener(e -> {
            if (joinedRooms.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "You are not a member of any room.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Prvo biramo sobu
            String room = (String) JOptionPane.showInputDialog(
                    frame,
                    "Select a room to invite user to:",
                    "Invite User",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    joinedRooms.toArray(),
                    null
            );

            if (room != null) {
                String userToInvite = JOptionPane.showInputDialog(frame, "Enter username to invite:");
                if (userToInvite != null && !userToInvite.trim().isEmpty()) {
                    chatClient.sendObject(new rs.raf.pds.v4.z5.messages.InviteUser(room, userToInvite.trim()));
                }
            }
        });

    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            chatClient.sendObject(new ChatMessage(username, text));
            displayMessage(username + ": " + text);
            inputField.setText("");
        }
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("Server:") && message.contains("is not available")) {
                JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Error", JOptionPane.ERROR_MESSAGE);
            } else if (message.startsWith("Server:Available rooms:")) {
                availableRooms.clear();
                String roomsPart = message.substring("Server:Available rooms:".length()).trim();
                if (!roomsPart.isEmpty()) {
                    String[] rooms = roomsPart.split(",");
                    for (String room : rooms) {
                        availableRooms.add(room.trim());
                    }
                }
                appendColoredText(message + "\n", Color.BLACK);

                if (awaitingRoomList) {
                    awaitingRoomList = false;
                    showJoinRoomDialog();
                }
            } 
            else if (message.startsWith("Server:No rooms available")) {
                availableRooms.clear();
                appendColoredText(message + "\n", Color.BLACK);
                if (awaitingRoomList) {
                    awaitingRoomList = false;
                    JOptionPane.showMessageDialog(frame, "No rooms available to join.", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } 
            else if (message.startsWith("New room available:")) {
                String newRoom = message.substring("New room available:".length()).trim();
                if (!availableRooms.contains(newRoom)) {
                    availableRooms.add(newRoom);
                }
                appendColoredText("Server: New room available: " + newRoom + "\n", Color.BLACK);
            } 
            else if (message.startsWith("Server:You have joined the room ")) {
                String roomName = message.substring("Server:You have joined the room ".length()).split("\\.")[0].trim();
                if (!joinedRooms.contains(roomName)) {
                    joinedRooms.add(roomName);
                    updateJoinedRoomsDisplay();
                }
                appendColoredText(message + "\n", Color.BLACK);
            }

            else if (message.startsWith("Server:You have been invited to room ")) {
                String roomName = message.substring("Server:You have been invited to room ".length()).replace(".", "").trim();
                int result = JOptionPane.showConfirmDialog(frame,
                        "You have been invited to join room: " + roomName + "\nDo you want to join?",
                        "Room Invitation",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chatClient.sendObject(new JoinRoom(roomName, username));
                }
            }
            else if (message.startsWith("Server:Room ") && message.contains(" created, and you joined it.")) {
                String part = message.substring("Server:Room ".length());
                String roomName = part.split(" created")[0].trim();
                if (!joinedRooms.contains(roomName)) {
                    joinedRooms.add(roomName);
                    updateJoinedRoomsDisplay();
                }
                appendColoredText(message + "\n", Color.BLACK);
            }

            
            else if (message.startsWith(username + ":")) {
                appendColoredText(message + "\n", Color.BLUE);
            } else if (message.startsWith("Server:")) {
                if (message.contains("is already a member") ||
                        message.contains("does not exist") ||
                        message.contains("is not online") ||
                        message.contains("You must be a member") ||
                        message.contains("Error:") ||
                        message.contains("is already connected") ||
                        message.contains("has connected")) {

                        JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        appendColoredText(message + "\n", Color.BLACK);
                    }
                }
            else {
                appendColoredText(message + "\n", Color.RED);
            }
        });
    }

    private void showJoinRoomDialog() {
        if (availableRooms.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No rooms available to join.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(frame, "Join Room", true);
        dialog.setLayout(new GridLayout(availableRooms.size(), 1));

        boolean addedAnyRoom = false;
        for (String roomName : availableRooms) {
            if (joinedRooms.contains(roomName)) continue;
            JButton roomButton = new JButton(roomName);
            roomButton.addActionListener(ev -> {
                chatClient.sendObject(new JoinRoom(roomName, username));
                dialog.dispose();
            });
            dialog.add(roomButton);
            addedAnyRoom = true;
        }

        if (!addedAnyRoom) {
            JOptionPane.showMessageDialog(frame, "You have already joined all available rooms.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void appendColoredText(String text, Color color) {
        StyledDocument doc = chatPane.getStyledDocument();
        Style style = chatPane.addStyle("ColorStyle", null);
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateJoinedRoomsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (String room : joinedRooms) {
            sb.append(room).append("\n");
        }
        joinedRoomsArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.trim().isEmpty()) {
            try {
                ChatClient chatClient = new ChatClient("localhost", 4555, username.trim());
                new ChatClientGUI(chatClient);
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
