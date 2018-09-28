package main.java.Util;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Message{

	private final static String COMMAND = "MSG_COMMAND"; 
	private final static String CONTENT = "MSG_CONTENT";
	private final static String TYPE = "SERVER_TYPE"; 

	private String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	private String contents=null;
	private String server_type="ALL"; // which server to execute remote interface

	private String prefix="";

	public Message(String msg_type){
		this.msg_type = msg_type;
		if(msg_type.contains("reserve") || msg_type.contains("Customer")) {
			prefix="customer_";
		}
		else {			
			if (msg_type.contains("Flight")) {
				prefix="flight_";
				server_type="Flight";
			}
			else if (msg_type.contains("Car")) {
				prefix="car_";server_type="Car";
			}
			else if (msg_type.contains("Room")) {
				prefix="room_";
				server_type="Room";
			}
		}
	}

	/*
	return something like:
		{
			"MSG_COMMAND": "addFlights",
			"SERVER_TYPE": "Flight",
			"MSG_CONTENT": {"id":2,"location":"montreal"} 
		}
	*/
	@Override
	public String toString(){
		JSONObject obj = null;
		try{
			obj = new JSONObject();
			obj.accumulate(COMMAND, msg_type);
			obj.accumulate(TYPE, server_type);
			obj.accumulate(CONTENT, contents);
		}
		catch (JSONException e){
			System.err.println("ERROR:: Message.java: JSON.toString failed for COMMAND="+msg_type+"TYPE="+server_type+CONTENT+"=contents");
			e.printStackTrace();
		}
		
		return obj.toString();
	}

	// encode an <AddFlights> command
	public void addFlightCommand(int id, int flightNum, int flightSeats, int flightPrice) throws JSONException {
		this.contents = (new JSONObject())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"Num",flightNum)
						.accumulate(prefix+"Seats",flightSeats)
						.accumulate(prefix+"Price",flightPrice).toString();
	}

	// encode <AddCars> or <AddRooms> command
	public void addCommand(int id, String location, int nums, int price) throws JSONException{
		this.contents = (new JSONObject())
						.accumulate(prefix+"id", id)
						.accumulate(prefix+"location", location)
						.accumulate(prefix+"nums", nums)
						.accumulate(prefix+"price", price).toString();
	}

	// encode <deleteFlight> command
	public void delOrQueryFlightCommand(int id, int flightNum) throws JSONException{
		this.contents = (new JSONObject())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"Num",flightNum).toString();
	}

	// encode <deleteCars> or <deleteRooms>, <queryCars> or <queryRooms>, <queryCarsPrice> or <queryRPrice>
	public void delOrQueryCommand(int id, String location) throws JSONException{
		this.contents = (new JSONObject())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"location",location).toString();
	}
	
	
	// encode <bundle>
	public void bundleCommand(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
		throws JSONException {
		JSONArray flightNums = new JSONArray();
		for (String fn :flightNumbers) flightNums.put(fn);
		this.contents = (new JSONObject()).accumulate(prefix+"id",id)
						.accumulate(prefix+"customerID",customerID)
						.put(prefix+"flightNumbers",flightNums)
						.accumulate(prefix+"location",location)
						.accumulate(prefix+"car",car)
						.accumulate(prefix+"room",room).toString();
	}

	// encode <addCustomer>, <deleteCusmoter> and <queryCustomer> with id and cid
	public void addDeleteQueryCustomerCommand(int id, int cid) throws JSONException{
		this.contents = (new JSONObject()).accumulate(prefix+"id",id)
						.accumulate(prefix+"customerID",cid).toString();
	}

	// encode <addCustomer> with id only
	public void addCustomerCommand(int id) throws JSONException{
		this.contents = (new JSONObject()).accumulate(prefix+"id",id).toString();
	}

	
	public void reserveFlightCommand(int id, int customerID, int flightNumber){
		this.contents = (new JSONObject()).accumulate(prefix+"id",id)
						.accumulate(prefix+"customerID",customerID)
						.accumulate(prefix+"flightNumber",flightNumber).toString();
	}
	

	public void reserveCommand(int id,int customerID, String location){
		this.contents = (new JSONObject()).accumulate(prefix+"id",id)
						.accumulate(prefix+"customerID",customerID)
						.accumulate(prefix+"location",location).toString();
	}
	// TODO: more encoding methods....


}