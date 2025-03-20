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

    private List<String> availableRooms = new ArrayList<>();
    private List<String> joinedRooms = new ArrayList<>();
    private boolean awaitingRoomList = false;

    public ChatClientGUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.username = chatClient.userName;

        frame = new JFrame("Chat App - " + username);
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        //  prikaz poruka
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        frame.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // unos poruka
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // dugmici za sobe
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createRoomButton = new JButton("Create Room");
        JButton joinRoomButton = new JButton("Join Room");
        topPanel.add(createRoomButton);
        topPanel.add(joinRoomButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Listener za dolazne poruke
        chatClient.setOnMessageReceivedListener(this::displayMessage);

        // Slanje poruka na klik
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Kreiranje sobe
        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(frame, "Enter room name:");
            if (roomName != null && !roomName.trim().isEmpty()) {
                chatClient.sendObject(new CreateRoom(roomName.trim(), username));
            }
        });

        // Join room (traži liste soba)
        joinRoomButton.addActionListener(e -> {
            awaitingRoomList = true;
            chatClient.sendObject(new InfoMessage("GET_ROOMS"));
        });

        frame.setVisible(true);
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

            } else if (message.startsWith("Server:No rooms available")) {
                availableRooms.clear();
                appendColoredText(message + "\n", Color.BLACK);
                if (awaitingRoomList) {
                    awaitingRoomList = false;
                    JOptionPane.showMessageDialog(frame, "No rooms available to join.", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (message.startsWith("New room available:")) {
                String newRoom = message.substring("New room available:".length()).trim();
                if (!availableRooms.contains(newRoom)) {
                    availableRooms.add(newRoom);
                }
                appendColoredText("Server: New room available: " + newRoom + "\n", Color.BLACK);
            } else if (message.startsWith("You have joined the room ")) {
                String roomName = message.substring("You have joined the room ".length()).split("\\.")[0].trim();
                if (!joinedRooms.contains(roomName)) {
                    joinedRooms.add(roomName);
                }
                appendColoredText(message + "\n", Color.BLACK);
            } else if (message.startsWith(username + ":")) {
                appendColoredText(message + "\n", Color.BLUE);
            } else if (message.startsWith("Server:")) {
                appendColoredText(message + "\n", Color.BLACK);
            } else {
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
