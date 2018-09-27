package Util;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


public class MessageDecoder{
	private final String COMMAND = "MSG_COMMAND"; 
	private final String CONTENT = "MSG_CONTENT";
	private final String TYPE = "SERVER_TYPE"; 

	private String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	private JSONObject args; // arguments to the message method

	public MessageDecoder(){

	}

	// return which server this command goes to
	public String decode_Type(String msgStr) throws JSONException {
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(TYPE);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeType: Cannot decode JSON message '"+msgStr+"'");
			e.printStackTrace();
			return "";
		}
	}

	// parse a JSON string 
	public String decode_Method(String msgStr){
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(COMMAND);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeMethod: Cannot decode JSON message '"+msgStr+"'");
			e.printStackTrace();
			return "";
		}
	}


	public class FlightMessageDecoder extends MessageDecoder{
		public int xid;
		public int flightNum;
		public int flightSeats;
		public int flightPrice;
		public String prefix = "flight_";

		// decode a "AddFlight" message
		public void decodeAddMsg(String msgStr){
			try{
				JSONObject obj = new JSONObject(msgStr);
				JSONObject contents = new JSONObject(obj.getString(CONTENT));
				xid = contents.getInt(prefix+"id");
				flightNum = contents.getInt(prefix+"Num");
				flightSeats = contents.getInt(prefix+"Seats");
				flightPrice = contents.getInt(prefix+"Price");
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeAddMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		// decode a "DeleteFlight" message
		public void decodeDelMsg(String msgStr){
			try{
				JSONObject obj = new JSONObject(msgStr);
				JSONObject contents = new JSONObject(obj.getString(CONTENT));
				xid = contents.getInt(prefix+"id");
				flightNum = contents.getInt(prefix+"Num");
				
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeDelMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		public void decodeQueryMsg(String msgStr){
			try{
				JSONObject obj = new JSONObject(msgStr);
				JSONObject contents = new JSONObject(obj.getString(CONTENT));
				xid = contents.getInt(prefix+"id");
				flightNum = contents.getInt(prefix+"Num");
			
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeQueryMsg: Cannot decode JSON message '"+msgStr+"'");
			}
		}

	}

	// TODO: other messageDecoder subclasses that decodes <Car>/<Room>/<Customer> message
	

}