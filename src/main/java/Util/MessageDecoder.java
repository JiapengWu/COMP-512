package main.java.Util;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MessageDecoder{
	private final static String COMMAND = "MSG_COMMAND"; 
	private final static String CONTENT = "MSG_CONTENT";
	private final static String SERVER_TYPE = "SERVER_TYPE"; 

	private final static String JSON_EXCEPTION = "<JSONException>";

	protected String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	protected JSONObject args; // arguments to the message method

	// return which server this command goes to
	public static String getServerType(String msgStr) throws JSONException {
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(SERVER_TYPE);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeType: Cannot decode JSON message '" + msgStr + "'");
			e.printStackTrace();
			return JSON_EXCEPTION;
		}
	}

	// return the name of the command
	public static String getCommand(String msgStr){
		try{
			JSONObject obj = new JSONObject(msgStr);
			return obj.getString(COMMAND);
		}
		
		catch (JSONException e){
			System.err.println("ERROR:: MessageDecoder.decodeMethod: Cannot decode JSON message '" + msgStr + "'");
			e.printStackTrace();
			return JSON_EXCEPTION;
		}
	}

	// return the argument part of the command
	public static String getContent(String msgStr){
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
		public int id;
		public int flightNum;
		public int flightSeats;
		public int flightPrice;
		public int customerID;


		// decode a "AddFlight" message
		public void decodeAddMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				flightNum = contents.getInt("Num");
				flightSeats = contents.getInt("Seats");
				flightPrice = contents.getInt("Price");
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeAddMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		// decode a "DeleteFlight" message
		public  void decodeDelOrQueryMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				flightNum = contents.getInt("Num");
				
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeDelMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		public void decodeReserveMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				flightNum = contents.getInt("flightNumber");
				customerID = contents.getInt("customerID");
			
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeReserveMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

	}
	
	public class RoomCarMessageDecoder extends MessageDecoder{
		public int id;
		public String location;
		public int nums;
		public int price;
		public int customerID;
		

		// decode a "AddFlight" message
		public void decodeAddMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				location = contents.getString("location");
				nums = contents.getInt("nums");
				price = contents.getInt("price");
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeAddMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		// decode a "DeleteFlight" message
		public  void decodeDelOrQueryMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				location = contents.getString("location");
				
			}
			catch (JSONException e){
				System.err.println("ERROR:: FlightMessageDecoder.decodeDelMsg: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
		}

		public void decodeReserveMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				location = contents.getString("location");
				customerID = contents.getInt("customerID");
			
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
		

		public void decodeCommandMsg(String msgStr){
			try{
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				customerID = contents.getInt("customerID");
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
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
			}
			catch (JSONException e){
				System.err.println("ERROR:: CustomerMessageDecoder.decodeCommand: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
				}
			}
		
	}
	
	public class BundleMessageDecoder extends MessageDecoder{

		public int id;
		public int customerID;
		public Vector<String> flightNums = new Vector<String>();
		public String location;
		public boolean car;
		public boolean room;
		
		
		public void decodeCommandMsg(String msgStr){
			try {				
				JSONObject contents = new JSONObject(msgStr);
				id = contents.getInt("id");
				customerID = contents.getInt("customerID");
				JSONArray jsonArray = contents.getJSONArray("flightNumbers");
				if (jsonArray != null) { 
				   for (int i=0;i<jsonArray.length();i++){ 
					   flightNums.add(jsonArray.getString(i));
				   } 
				} 
				location = contents.getString("location");
				car = contents.getBoolean("car");
				room = contents.getBoolean("room");
			}catch (JSONException e){
				System.err.println("ERROR:: BundleMessageDecoder.decodeCommand: Cannot decode JSON message '"+msgStr+"'");
				e.printStackTrace();
			}
			catch (NullPointerException e){
				System.err.println("ERROR:: BundleMessageDecoder.decodeCommand: NullPointerException '"+msgStr+"'");
				e.printStackTrace();
			}
		}
	}
	


}