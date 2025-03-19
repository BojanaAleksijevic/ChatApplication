package rs.raf.pds.v4.z5;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
//import java.text.NumberFormat.Style;
import javax.swing.text.Style;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.CreateRoom;
import rs.raf.pds.v4.z5.messages.RoomMessage;

public class ChatClientGUI {
    private ChatClient chatClient;
    private JFrame frame;
    private JTextPane chatPane; 
    private JTextField inputField;
    private JButton sendButton;
    private String username;

    public ChatClientGUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.username = chatClient.userName;

        frame = new JFrame("Chat App - " + username);
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Prostor za prikaz poruka
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        frame.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // Panel za unos poruka
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);
        
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createRoomButton = new JButton("Create Room");
        
        topPanel.add(createRoomButton);
        
        frame.add(topPanel, BorderLayout.NORTH);

        
        // Postavljamo listener da GUI reaguje na dolazne poruke
        chatClient.setOnMessageReceivedListener(this::displayMessage);
        
        // Slanje poruka na klik ili na enter
        sendButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                chatClient.sendObject(new ChatMessage(username, text));
                displayMessage(username + ": " + text);
                inputField.setText("");
            }
        });
        
        inputField.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                chatClient.sendObject(new ChatMessage(username, text));
                displayMessage(username + ": " + text);  // <-- ovo dodaj
                inputField.setText("");
            }
        });
        
        // kreiranje sobe
        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(frame, "Enter room name:");
            if (roomName != null && !roomName.trim().isEmpty()) {
                chatClient.sendObject(new CreateRoom(roomName, username));
               // chatArea.append("Created room: " + roomName + "\n");
            }
        });
        
        
       



        frame.setVisible(true);
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("Server:") && message.contains("is not available")) {
                JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Greška", JOptionPane.ERROR_MESSAGE);
            } else if (message.startsWith(username + ":")) {
                appendColoredText(message + "\n", Color.BLUE);
            } else if (message.startsWith("Server:")) {
                appendColoredText(message + "\n", Color.BLACK);
            } else {
                appendColoredText(message + "\n", Color.RED);
            }
        });
    }
    
    // Funkcija za ubacivanje obojenog teksta u JTextPane
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
                ChatClient chatClient = new ChatClient("localhost", 4555, username);
                new ChatClientGUI(chatClient);
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
