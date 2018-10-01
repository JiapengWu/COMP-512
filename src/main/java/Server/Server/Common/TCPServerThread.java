package main.java.Server.Server.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import main.java.Util.MessageDecoder;
import main.java.Util.MessageDecoder.BundleMessageDecoder;
import main.java.Util.MessageDecoder.CustomerMessageDecoder;
import main.java.Util.MessageDecoder.FlightMessageDecoder;
import main.java.Util.MessageDecoder.RoomCarMessageDecoder;

public class TCPServerThread implements Runnable {
	private Socket mw_socket;
	private String s_serverName;
	private ResourceManager rm;

	public TCPServerThread(Socket mw_socket, String s_serverName, ResourceManager rm) {
		this.mw_socket = mw_socket;
		this.s_serverName = s_serverName;
		this.rm = rm;
	}

	@Override
	public void run() {

		BufferedReader fromMW = null;
		PrintWriter toMW = null;
		try {
			fromMW = new BufferedReader(new InputStreamReader(mw_socket.getInputStream()));
			toMW = new PrintWriter(mw_socket.getOutputStream(), true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		MessageDecoder decoder = new MessageDecoder();

		String msg = null;
		// listen to middleware
		try {
			while ((msg = fromMW.readLine()) != null) {
				String res = "";

				try {
					// the actual shit happening here: parse the message, call RM method, write back
					// to middleware
					String command = MessageDecoder.getCommand(msg);
					String content = MessageDecoder.getContent(msg);
					FlightMessageDecoder flightMsgDecoder;
					CustomerMessageDecoder customerMsgDecoder;
					RoomCarMessageDecoder roomCarMsgDecoder;
					BundleMessageDecoder bundleMsgDecoder;
					switch (command) {
					case "addFlight":
						flightMsgDecoder = decoder.new FlightMessageDecoder();
						flightMsgDecoder.decodeAddMsg(content);
						
						res = Boolean.toString(rm.addFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum,
								flightMsgDecoder.flightSeats, flightMsgDecoder.flightPrice));
						break;

					case "addCars":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeAddMsg(content);
						res = Boolean.toString(rm.addCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location,
								roomCarMsgDecoder.nums, roomCarMsgDecoder.price));
						break;
					case "addRooms":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeAddMsg(content);
						res = Boolean.toString(rm.addRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location,
								roomCarMsgDecoder.nums, roomCarMsgDecoder.price));
						break;
					case "deleteFlight":
						flightMsgDecoder = decoder.new FlightMessageDecoder();
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum));
						break;
					case "deleteCars":
						roomCarMsgDecoder =decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "deleteRooms":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "queryFlight":
						flightMsgDecoder = decoder.new FlightMessageDecoder();
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum));
						break;
					case "queryCars":
						roomCarMsgDecoder =decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "queryRooms":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "queryFlightPrice":
						flightMsgDecoder = decoder.new FlightMessageDecoder();
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryFlightPrice(flightMsgDecoder.id, flightMsgDecoder.flightNum));
						break;
					case "queryCarsPrice":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryCarsPrice(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "queryRoomsPrice":
						roomCarMsgDecoder =decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryRoomsPrice(roomCarMsgDecoder.id, roomCarMsgDecoder.location));
						break;
					case "reserveFlight":
						flightMsgDecoder = decoder.new FlightMessageDecoder();
						flightMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveFlight(flightMsgDecoder.id, flightMsgDecoder.customerID,
								flightMsgDecoder.flightNum));
						break;
					case "reserveCar":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveCar(roomCarMsgDecoder.id, roomCarMsgDecoder.customerID,
								roomCarMsgDecoder.location));
						break;
					case "reserveRoom":
						roomCarMsgDecoder = decoder.new RoomCarMessageDecoder();
						roomCarMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveRoom(roomCarMsgDecoder.id, roomCarMsgDecoder.customerID,
								roomCarMsgDecoder.location));
						break;
					case "newCustomer":
						customerMsgDecoder = decoder.new CustomerMessageDecoder();
						customerMsgDecoder.decodeCommandMsg(content);
						res = Boolean.toString(rm.newCustomer(customerMsgDecoder.id, customerMsgDecoder.customerID));
						break;
					case "newCustomerID":
						customerMsgDecoder = decoder.new CustomerMessageDecoder();
						customerMsgDecoder.decodeCommandMsg(content);
						res = Boolean.toString(rm.newCustomer(customerMsgDecoder.id, customerMsgDecoder.customerID));
						break;
					case "deleteCustomer":
						customerMsgDecoder = decoder.new CustomerMessageDecoder();
						customerMsgDecoder.decodeCommandMsg(content);
						res = Boolean.toString(rm.deleteCustomer(customerMsgDecoder.id, customerMsgDecoder.customerID));
						break;
					case "queryCustomerInfo":
						customerMsgDecoder = decoder.new CustomerMessageDecoder();
						customerMsgDecoder.decodeCommandMsg(content);
						res = rm.queryCustomerInfo(customerMsgDecoder.id, customerMsgDecoder.customerID);
						break;
					case "bundle":
						bundleMsgDecoder = decoder.new BundleMessageDecoder();
						bundleMsgDecoder.decodeCommandMsg(content);
						res = Boolean.toString(rm.bundle(bundleMsgDecoder.id, bundleMsgDecoder.customerID, bundleMsgDecoder.flightNums,
								bundleMsgDecoder.location, bundleMsgDecoder.car, bundleMsgDecoder.room));
						break;
						// if command doesn't match any of the above
					default:
						res = "<IllegalArgumentException>";
					}

				} catch (IOException e) {
					Trace.error("Server " + s_serverName + " get IOException");
					res = "IOException";
				} catch (IllegalArgumentException e) {
					Trace.error("Server " + s_serverName + " get IllegalArgumentException");
					res = "IllegalArgumentException";
				}
				Trace.info("Command output = "+res);
				toMW.println(res);
				toMW.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}