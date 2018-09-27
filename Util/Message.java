package Util;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Vector;

public class Message{

	private final String COMMAND = "MSG_COMMAND"; 
	private final String TYPE = "SERVER_TYPE"; 
	private final String CONTENT = "MSG_CONTENT";

	private String msg_type; // method name this message contains, eg, "addFlights","queryCar"...
	private String contents=null;
	private String server_type=""; // which server to execute remote interface

	private String prefix="";

	public Message(String msg_type){
		this.msg_type = msg_type;
		if (msg_type.contains("Flight")) {prefix="flight_";server_type="Flight";}
		else if (msg_type.contains("Car")) {prefix="car_";server_type="Car";}
		else if (msg_type.contains("Room")) {prefix="room_";server_type="Room";}
		else if (msg_type.contains("Customer")) prefix="customer_";
		else prefix="bundle_"; 
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
		JSONObject obj = new JSONObject();
		obj.accumulate(COMMAND, msg_type);
		obj.accumulate(TYPE, server_type);
		obj.accumulate(CONTENT, contents);
		return obj.toString();
	}

	// encode an <AddFlights> command
	public void addFlightCommand(int id, int flightNum, int flightSeats, int flightPrice){
		this.contents = (new JSONObject())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"Num",flightNum)
						.accumulate(prefix+"Seats",flightSeats)
						.accumulate(prefix+"Price",flightPrice).toString();
	}

	// encode <AddCars> or <AddRooms> command
	public void addCommand(int id, String location, int nums, int price){
		this.contents = (new JSONObject())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"location",location)
						.accumulate(prefix+"nums",nums)
						.accumulate(prefix+"price",price).toString();
	}

	// encode <deleteFlight> command
	public void delFlightCommand(int id, int flightNum){
		this.contents = (new JSONObejct())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"Num",flightNum).toString();
	}

	// encode <deleteCars> or <deleteRooms>
	public void delCommand(int id, String location){
		this.contents = (new JSONObejct())
						.accumulate(prefix+"id",id)
						.accumulate(prefix+"location",location).toString();
	}

	// encode <bundle>
	public void bundleCommand(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room){
		JSONArray flightNums = new JSONArray();
		for (String fn :flightNumbers) flightNums.put(fn);
		this.contents = (new JSONObject()).accumulate("id",id)
						.accumulate(prefix+"customerID",customerID)
						.put(prefix+"flightNumbers",flightNumbers)
						.accumulate(prefix+"location",location)
						.accumulate(prefix+"car",car)
						.accumulate(prefix+"room",room).toString();
	}

	// encode <addCustomer> with id
	public void addCustomerCommand(int id){
		this.contents = (new JSONObejct()).accumulate(prefix+"id",id).toString();
	}

	// more encoding methods....


}