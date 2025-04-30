package rs.raf.pds.v4.z5;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rs.raf.pds.v4.z5.ChatClient.RoomMessageListener;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.CreateRoom;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.InviteUser;
import rs.raf.pds.v4.z5.messages.JoinRoom;
import rs.raf.pds.v4.z5.messages.MessageOffset;
import rs.raf.pds.v4.z5.messages.ReplyMessage;
import rs.raf.pds.v4.z5.messages.RoomMessage;

public class ChatClientGUI implements RoomMessageListener {
    private ChatClient chatClient;
    private JFrame frame;
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private String username;
    private JTextArea joinedRoomsArea;
    private JComboBox<String> roomComboBox;
    
    
    private boolean loadingHistory = false;


    private List<String> availableRooms = new ArrayList<>();
    private List<String> joinedRooms = new ArrayList<>();
    private boolean awaitingRoomList = false;
    
    
    private List<MessageOffset> messageOffsets = new ArrayList<>();
    private StyledDocument doc;
    private RoomMessage lastAppendedMessage = null;

    private int replyMessageId = -1;
    private String replyText = null;
    private JLabel replyToLabel;
    private JButton cancelReplyButton;

    private JLabel statusLabel;


    public ChatClientGUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.username = chatClient.userName;

        frame = new JFrame("Chat App - " + username);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Gornji panel sa dugmićima i prikazom soba
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
        JLabel roomsLabel = new JLabel("Rooms you are a member of:");
        joinedRoomsArea = new JTextArea(3, 50);
        joinedRoomsArea.setEditable(false);
        joinedRoomsArea.setLineWrap(true);
        joinedRoomsArea.setWrapStyleWord(true);
        JScrollPane roomsScroll = new JScrollPane(joinedRoomsArea);
        roomsPanel.add(roomsLabel, BorderLayout.NORTH);
        roomsPanel.add(roomsScroll, BorderLayout.CENTER);
        
        
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED); // ili neka druga boja
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        roomsPanel.add(statusLabel, BorderLayout.SOUTH); 

  
        

        topPanel.add(buttonPanel);
        topPanel.add(roomsPanel);
        frame.add(topPanel, BorderLayout.NORTH);
       

        // Chat prikaz
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        frame.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        
        doc = chatPane.getStyledDocument();

        chatPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
            	String selectedRoom = (String) roomComboBox.getSelectedItem(); 
            	System.out.println("Kliknuto na listu!"); 
                int pos = chatPane.viewToModel2D(e.getPoint());
                System.out.println("Kliknuto na poziciji u tekstu: " + pos);
                System.out.println("Ukupno offseta: " + messageOffsets.size());

                for (MessageOffset mo : messageOffsets) {
                    System.out.println("Provera [" + mo.getStartOffset() + ", " + mo.getEndOffset() + "]");
                    System.out.println("[" + mo.getStartOffset() + ", " + mo.getEndOffset() + "]: " + mo.getMessage().getTxt());
                    if (pos >= mo.getStartOffset() && pos <= mo.getEndOffset()) {
                        RoomMessage clickedMessage = mo.getMessage();
                        if (!clickedMessage.getRoomName().equals(selectedRoom)) {
                            // Kliknuta poruka NIJE iz trenutno izabrane sobe — ignorisi
                            System.out.println("Poruka nije iz trenutne sobe, reply onemogućen.");
                            JOptionPane.showMessageDialog(chatPane,
                            	    "Ne možete odgovoriti na poruku iz druge sobe.",
                            	    "Greška pri odgovoru",
                            	    JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        replyText = clickedMessage.getTxt();
                        replyMessageId = clickedMessage.getId();

                        // Skrati prikaz teksta (prvih 30 karaktera)
                        String prikazaniTekst = replyText.length() > 30 ? replyText.substring(0, 30) + "..." : replyText;
                        System.out.println("Kliknuta poruka: " + clickedMessage.getTxt());

                        replyToLabel.setText("<html>Replying to:<br>" + prikazaniTekst + "</html>");
                        replyToLabel.setVisible(true);
                        cancelReplyButton.setVisible(true);
                        break;
                    }
                }
            }
        });


        
        
        // Donji panel - reply i input
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        
     // Panel za labelu za odgovor i X dugme
        JPanel replyPanel = new JPanel(new BorderLayout());
        replyPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // margine oko panela

        replyToLabel = new JLabel(" ");
        replyToLabel.setForeground(Color.GRAY);
        replyToLabel.setVisible(false);
        replyPanel.add(replyToLabel, BorderLayout.CENTER); // labela levo

        cancelReplyButton = new JButton("X");
        cancelReplyButton.setBackground(Color.RED);
        cancelReplyButton.setForeground(Color.WHITE);
        cancelReplyButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10)); // padding u dugmetu
        cancelReplyButton.setFocusable(false);
        cancelReplyButton.setVisible(false);

        cancelReplyButton.addActionListener(e -> {
        	resetReply();
        });

        replyPanel.add(cancelReplyButton, BorderLayout.EAST); // dugme skroz desno
        bottomPanel.add(replyPanel, BorderLayout.NORTH);


        
        // input panel za unos poruka
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        JButton loadHistoryButton = new JButton("Load History");

        roomComboBox = new JComboBox<>();
        roomComboBox.addItem("Global Chat"); // inicijalno
        
        JPanel bottomRightPanel = new JPanel(new BorderLayout());
        bottomRightPanel.add(roomComboBox, BorderLayout.NORTH);
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 5, 5)); // 2 dugmeta, jedno ispod drugog
        buttonsPanel.add(loadHistoryButton);
        buttonsPanel.add(sendButton);
      

        bottomRightPanel.add(buttonsPanel, BorderLayout.SOUTH);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(bottomRightPanel, BorderLayout.EAST);

        // inputPanel ispod reply labele
        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        // ceo donji deo u frame
        frame.add(bottomPanel, BorderLayout.SOUTH);

     // Listener za dolazne poruke
        //chatClient.setOnRoomMessageReceivedListener(this);

        chatClient.setOnRoomMessageReceivedListener((RoomMessage roomMessage) -> {
            if (!roomMessage.getUser().equals(username)) {
                if (roomMessage instanceof ReplyMessage) {
                    ReplyMessage replyMsg = (ReplyMessage) roomMessage;

                    String prikazaniTekst = replyMsg.getReplyToText();
                    if (prikazaniTekst.length() > 30) {
                        prikazaniTekst = prikazaniTekst.substring(0, 30) + "...";
                    }

                    displayMessage(roomMessage.getUser() + " (reply to: " + prikazaniTekst + "): " + roomMessage.getTxt(), roomMessage.getRoomName());
                } else {
                    displayMessage(roomMessage.getUser() + ": " + roomMessage.getTxt(), roomMessage.getRoomName());
                }

                addMessage(roomMessage);
            }
        });


        chatClient.setOnTextMessageReceivedListener((String message) -> {
        	if (loadingHistory) {
                displayMessage(message, "");
            } else {
                displayMessage(message, "Global Chat");
            }
        });

        
        
        
        // Slanje poruke na dugme i enter
     // Slanje poruke na klik dugmeta
        sendButton.addActionListener(e -> sendMessage());

        // Slanje poruke pritiskom na Enter
        inputField.addActionListener(e -> sendMessage());


        // Kreiranje sobe
        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(frame, "Enter room name:");
            if (roomName != null && !roomName.trim().isEmpty()) {
                chatClient.sendObject(new CreateRoom(roomName.trim(), username));
            }
        });

        // Join room
        joinRoomButton.addActionListener(e -> {
            awaitingRoomList = true;
            chatClient.sendObject(new InfoMessage("GET_ROOMS"));
        });

        // Invite user
        inviteUserButton.addActionListener(e -> {
            if (joinedRooms.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "You are not a member of any room.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

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
                    chatClient.sendObject(new InviteUser(room, userToInvite.trim()));
                }
            }
        });
        
        // prikaz svih poruka iz sobe
        loadHistoryButton.addActionListener(e -> {
            String selectedRoom = (String) roomComboBox.getSelectedItem();
            if (!"Global Chat".equals(selectedRoom)) {
                loadingHistory = true; // počinje učitavanje
                chatClient.sendObject("/getAllMessages " + selectedRoom);

              //  chatClient.sendObject(new InfoMessage("/getAllMessages " + selectedRoom));
            } else {
                JOptionPane.showMessageDialog(frame, "Global Chat nema čuvanu istoriju.");
            }
        });




        frame.setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            String selectedRoom = (String) roomComboBox.getSelectedItem();

            if ("Global Chat".equals(selectedRoom)) {
                ChatMessage message = new ChatMessage(username, text);
                chatClient.sendObject(message);
                appendColoredText(username + ": " + text + "\n", Color.GREEN);

            } else {
                RoomMessage msg;
                if (replyText != null && replyMessageId != -1) {
                    msg = new RoomMessage(username, selectedRoom, text, replyMessageId, replyText);
                   // appendColoredText("[" + selectedRoom + "] " + username + " ↪ \"" + replyText + "\": " + text + "\n", Color.BLUE);
                } else {
                    msg = new RoomMessage(username, selectedRoom, text);
                   // appendColoredText("[" + selectedRoom + "] " + username + ": " + text + "\n", Color.BLUE);
                }
                chatClient.sendObject(msg);
            }

            resetReply();
            inputField.setText("");
        }
    }



    // Prikaz dolaznih poruka
    private void displayMessage(String message, String currentRoom) {
        SwingUtilities.invokeLater(() -> {
            String messageToDisplay = "";

            if (message.startsWith("Server:") && message.contains("is not available")) {
                JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Dodaj naziv sobe ispred poruke
            if (message.startsWith("Server:") || message.startsWith("Global Chat:")
                    || message.startsWith(currentRoom + ":") || message.startsWith(username + ":")
                    || message.startsWith(username + " (")) {
                messageToDisplay = message;
            } else if (currentRoom != null && !currentRoom.isEmpty() && !"Global Chat".equals(currentRoom)) {
                messageToDisplay = "[" + currentRoom + "] " + message;
            } else {
                messageToDisplay = message;
            }

            if (message.startsWith("Server:Available rooms:")) {
                availableRooms.clear();
                String roomsPart = message.substring("Server:Available rooms:".length()).trim();
                if (!roomsPart.isEmpty()) {
                    String[] rooms = roomsPart.split(",");
                    for (String room : rooms) {
                        availableRooms.add(room.trim());
                    }
                }
                appendColoredText(messageToDisplay + "\n", Color.BLACK);

                if (awaitingRoomList) {
                    awaitingRoomList = false;
                    showJoinRoomDialog();
                }
            } else if (message.startsWith(username + " (reply to:")) {
                String[] parts = message.split(":");
                String replyInfo = parts[0].replace(" (reply to: ", "");
                String replyMessage = parts[1].trim();

                messageToDisplay = username + " (reply to: " + replyInfo + "): " + replyMessage;
            } else if (message.startsWith("Server:No rooms available")) {
                availableRooms.clear();
                appendColoredText(messageToDisplay + "\n", Color.BLACK);
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
            } else if (message.startsWith("Server:You have joined the room ")) {
                String roomName = message.substring("Server:You have joined the room ".length()).split("\\.")[0].trim();
                if (!joinedRooms.contains(roomName)) {
                    joinedRooms.add(roomName);
                    updateJoinedRoomsDisplay();
                }
                appendColoredText(messageToDisplay + "\n", Color.BLACK);
            } else if (message.startsWith("Server:You have been invited to room ")) {
                String roomName = message.substring("Server:You have been invited to room ".length()).replace(".", "").trim();
                int result = JOptionPane.showConfirmDialog(frame,
                        "You have been invited to join room: " + roomName + "\nDo you want to join?",
                        "Room Invitation",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chatClient.sendObject(new JoinRoom(roomName, username));
                }
            } else if (message.startsWith("Server:Room ") && message.contains(" created, and you joined it.")) {
                String part = message.substring("Server:Room ".length());
                String roomName = part.split(" created")[0].trim();
                if (!joinedRooms.contains(roomName)) {
                    joinedRooms.add(roomName);
                    updateJoinedRoomsDisplay();
                }
                appendColoredText(messageToDisplay + "\n", Color.BLACK);
            } else if (message.startsWith(username + ":") || message.startsWith(username + " (")) {
                appendColoredText(messageToDisplay + "\n", Color.BLUE);
            } else if (message.startsWith("Global Chat:")) {
                appendColoredText(messageToDisplay + "\n", Color.GREEN);
            } else if (message.contains(currentRoom)) {
                appendColoredText(messageToDisplay + "\n", Color.ORANGE);
            } else if (message.startsWith("Server:")) {
                if (message.contains("is already a member") ||
                        message.contains("does not exist") ||
                        message.contains("is not online") ||
                        message.contains("You must be a member") ||
                        message.contains("Error:") ||
                        message.contains("is already connected") ||
                        message.contains("has connected") ||
                		message.contains("has joined the room")) {
                    JOptionPane.showMessageDialog(frame, message.substring(7).trim(), "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    appendColoredText(messageToDisplay + "\n", Color.BLACK);
                }
            } else {
                appendColoredText(messageToDisplay + "\n", Color.RED);
            }
        });
    }
    
    private void resetReply() {
        replyText = null;
        replyMessageId = -1;
        replyToLabel.setText(" ");
        replyToLabel.setVisible(false);
        cancelReplyButton.setVisible(false);
    }

    
    private void addMessage(RoomMessage message) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            int startOffset = doc.getLength();

            StringBuilder sb = new StringBuilder();
            if (message.getReplyToText() != null && !message.getReplyToText().isEmpty()) {
                sb.append("(reply to: ").append(message.getReplyToText()).append(")\n");
            }

            sb.append(message.getUser()).append(": ").append(message.getTxt()).append("\n\n");

            doc.insertString(doc.getLength(), sb.toString(), null);

            int endOffset = doc.getLength();

            messageOffsets.add(new MessageOffset(startOffset, endOffset, message));

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
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
        try {
            Style style = doc.addStyle("Style", null);
            StyleConstants.setForeground(style, color);

            int startOffset = doc.getLength(); // pre dodavanja
            doc.insertString(doc.getLength(), text, style);
            int endOffset = doc.getLength(); // posle dodavanja

            // Ako imamo lastAppendedMessage (npr. poslat ili primljen RoomMessage), sačuvaj njegov offset
            if (lastAppendedMessage != null) {
                messageOffsets.add(new MessageOffset(startOffset, endOffset, lastAppendedMessage));
                lastAppendedMessage = null; // resetuj ga da se ne koristi za sledeću poruku
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }




    
    private void updateRoomComboBox() {
        roomComboBox.removeAllItems();
        roomComboBox.addItem("Global Chat");
        for (String room : joinedRooms) {
            roomComboBox.addItem(room);
        }
    }


    private void updateJoinedRoomsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (String room : joinedRooms) {
            sb.append(room).append("\n");
        }
        joinedRoomsArea.setText(sb.toString());
        updateRoomComboBox();
    }
    
    @Override
    public void onRoomMessageReceived(RoomMessage roomMessage) {
        System.out.println("GUI: Poruka primljena: " + roomMessage.getTxt());
        try {
        	
        	if ("Server".equals(roomMessage.getUser())) {
        	    doc.insertString(doc.getLength(), roomMessage.getTxt() + "\n", null);
        	    return;
        	}

        	
            String roomPart = "[" + roomMessage.getRoomName() + "] ";
            String userPart = roomMessage.getUser();
            String separator, replyText = "", messageText;
            Color color;

            if (roomMessage.getReplyToMessageId() != 0) {
                separator = " ↪ \"" + roomMessage.getReplyToText() + "\": ";
                messageText = roomMessage.getTxt() + "\n";
                color = new Color(0, 102, 204); // svetloplava za reply
            } else {
                separator = ": ";
                messageText = roomMessage.getTxt() + "\n";
                color = new Color(0, 153, 0); // zelena za obične poruke u sobi
            }

            // Stil za običan tekst
            SimpleAttributeSet regularAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(regularAttr, color);

            // Stil za korisničko ime (bold + u istoj boji)
            SimpleAttributeSet boldAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(boldAttr, color);
            StyleConstants.setBold(boldAttr, true);

            int startOffset = doc.getLength();

            doc.insertString(doc.getLength(), roomPart, regularAttr);
            doc.insertString(doc.getLength(), userPart, boldAttr);
            doc.insertString(doc.getLength(), separator, regularAttr);
            doc.insertString(doc.getLength(), messageText, regularAttr);

            int endOffset = doc.getLength();

            messageOffsets.add(new MessageOffset(startOffset, endOffset, roomMessage));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }



    


    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.trim().isEmpty()) {
            try {
                ChatClient chatClient = new ChatClient("localhost", 4555, username.trim());
                ChatClientGUI gui = new ChatClientGUI(chatClient);

                chatClient.setOnRoomMessageReceivedListener(gui);
                chatClient.start();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


}