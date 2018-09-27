package message;

public class MessageDecoder{
	private final String COMMAND = "MSG_COMMAND"; 
	private final String CONTENT = "MSG_CONTENT";
	private final String TYPE = "SERVER_TYPE"; 

	private String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	private JSONobject args; // arguments to the message method

	public MessageDecoder(){

	}

	// return which server this command goes to
	public String decode_Type(String msgStr){
		JSONObject obj = new JSONObject(msgStr);
		return obj.getString(TYPE);
	}

	// parse a JSON string 
	public String decode_Method(String msgStr){
		JSONObject obj = new JSONObject(msgStr);
		return obj.getString(COMMAND);
	}
}