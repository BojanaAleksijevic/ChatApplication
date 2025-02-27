package rs.raf.pds.v4.z5.messages;

public class ChatMessage implements Message {
	private int id;
	String user;
	String txt;
	
	protected ChatMessage() {
		
	}
	
	public ChatMessage(String user, String txt) {
		this.user = user;
		this.txt = txt;
	}
	
	
	@Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

	public String getUser() {
		return user;
	}

	public String getTxt() {
		return txt;
	}
	
	
}
