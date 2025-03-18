package rs.raf.pds.v4.z5;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.RoomMessage;

public class ChatClientGUI {
    private ChatClient chatClient;
    private JFrame frame;
    private JTextArea chatArea;
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
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Panel za unos poruka
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Postavljamo listener da GUI reaguje na dolazne poruke
        chatClient.setOnMessageReceivedListener(this::displayMessage);
        
        // Slanje poruka na klik
        sendButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                chatClient.sendMessage(new ChatMessage(username, text));
                inputField.setText("");
            }
        });

        frame.setVisible(true);
    }

    private void displayMessage(String message) {
        System.out.println("GUI displayMessage: " + message);
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("Server:") && message.contains("is not available")) {
                // Izbacujemo popup prozor
                JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Greška", JOptionPane.ERROR_MESSAGE);
            } else {
                chatArea.append(message + "\n");
            }
        });
    }



    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.trim().isEmpty()) {
            try {
                ChatClient chatClient = new ChatClient("localhost", 4555, username);
                ChatClientGUI gui = new ChatClientGUI(chatClient);
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
