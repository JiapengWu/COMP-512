package main.java.Util;

import org.json.JSONException;
import org.json.JSONObject;


public class MessageDecoder{
	private final String COMMAND = "MSG_COMMAND"; 
	private final String CONTENT = "MSG_CONTENT";
	private final String TYPE = "SERVER_TYPE"; 

	private final String JSON_EXCEPTION = "<JSONException>";

	private String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	private JSONObject args; // arguments to the message method

	public MessageDecoder(){

	}

	// return which server this command goes to
	public String decodeType(String msgStr) throws JSONException {
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(TYPE);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeType: Cannot decode JSON message '"+msgStr+"'");
			e.printStackTrace();
			return JSON_EXCEPTION;
		}
	}

	// return the name of the command
	public String decodeCommand(String msgStr){
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(COMMAND);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeMethod: Cannot decode JSON message '"+msgStr+"'");
			e.printStackTrace();
			return JSON_EXCEPTION;
		}
	}

	// return the argument part of the command
	public String getContent(String msgStr){
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(CONTENT);
		}
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.getContent: Cannot decode JSON message '"+msgStr+"'");
			e.printStackTrace();
			return JSON_EXCEPTION;
		}
	}


	public class FlightMessageDecoder extends MessageDecoder{
		public int xid;
		public int flightNum;
		public int flightSeats;
		public int flightPrice;
		public int customerID;
		public String prefix = "flight_";

		// decode a "AddFlight" message
		public void decodeAddMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
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
				JSONObject contents = new JSONObject(msgStr);
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
				JSONObject contents = new JSONObject(msgStr);
				xid = contents.getInt(prefix+"id");
				flightNum = contents.getInt(prefix+"Num");
			
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeQueryMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		public void decodeReserveMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				xid = contents.getInt(prefix+"id");
				flightNum = contents.getInt(prefix+"flightNumber");
				customerID = contents.getInt(prefix+"customerID");
			
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeReserveMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

	}


	public class CustomerMessageDecoder extends MessageDecoder{
		public int id;
		public int customerID;
		String prefix = "customer_";

		public void decodeCommandMsg(String msgStr){
			try{
				JSONObejct contents = new JSONObject(msgStr);
				id = contents.getInt(prefix+"id");
				cid = contents.getInt(prefix+"customerID");
			}
			catch (JSONException e){
				System.err.println("ERROR:: CustomerMessageDecoder.decodeCommand: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
			catch (NullPointerException e){
				System.err.println("ERROR:: CustomerMessageDecoder.decodeCommand: NullPointerException '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		public void decodeCommandMsgNoCID(String msgStr){
			try{
				JSONObejct contents = new JSONObject(msgStr);
				id = contents.getInt(prefix+"id");
			catch (JSONException e){
				System.err.println("ERROR:: CustomerMessageDecoder.decodeCommand: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
				}
			}
	}


	// TODO: other messageDecoder subclasses that decodes <Car>/<Room> message


}