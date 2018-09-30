package main.java.Server.Server.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import main.java.Util.MessageDecoder;
import main.java.Util.MessageDecoder.CustomerMessageDecoder;
import main.java.Util.MessageDecoder.FlightMessageDecoder;
import main.java.Util.MessageDecoder.RoomCarMessageDecoder;

public class TCPServerThread implements Runnable {
	private Socket mw_socket;
	private String s_serverName;
	private ResourceManager rm;
	private ArrayList<Integer> customerIdx = new ArrayList<Integer>();

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
					switch (command) {
					case "addFlight":
						flightMsgDecoder = (FlightMessageDecoder) decoder;
						flightMsgDecoder.decodeAddMsg(content);
						// FIXME: On the same server, ResourceManager should be synchronized !
						res = Boolean.toString(rm.addFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum,
								flightMsgDecoder.flightSeats, flightMsgDecoder.flightPrice));

					case "addCars":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeAddMsg(content);
						res = Boolean.toString(rm.addCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location,
								roomCarMsgDecoder.nums, roomCarMsgDecoder.price));

					case "addRooms":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeAddMsg(content);
						res = Boolean.toString(rm.addRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location,
								roomCarMsgDecoder.nums, roomCarMsgDecoder.price));

					case "deleteFlight":
						flightMsgDecoder = (FlightMessageDecoder) decoder;
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum));

					case "deleteCars":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "deleteRooms":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Boolean.toString(rm.deleteRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "queryFlight":
						flightMsgDecoder = (FlightMessageDecoder) decoder;
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryFlight(flightMsgDecoder.id, flightMsgDecoder.flightNum));

					case "queryCars":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryCars(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "queryRooms":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryRooms(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "queryFlightPrice":
						flightMsgDecoder = (FlightMessageDecoder) decoder;
						flightMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryFlightPrice(flightMsgDecoder.id, flightMsgDecoder.flightNum));

					case "queryCarsPrice":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryCarsPrice(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "queryRoomsPrice":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeDelOrQueryMsg(content);
						res = Integer.toString(rm.queryRoomsPrice(roomCarMsgDecoder.id, roomCarMsgDecoder.location));

					case "reserveFlight":
						flightMsgDecoder = (FlightMessageDecoder) decoder;
						flightMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveFlight(flightMsgDecoder.id, flightMsgDecoder.customerID,
								flightMsgDecoder.flightNum));

					case "reserveCar":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveCar(roomCarMsgDecoder.id, roomCarMsgDecoder.customerID,
								roomCarMsgDecoder.location));

					case "reserveRoom":
						roomCarMsgDecoder = (RoomCarMessageDecoder) decoder;
						roomCarMsgDecoder.decodeReserveMsg(content);
						res = Boolean.toString(rm.reserveRoom(roomCarMsgDecoder.id, roomCarMsgDecoder.customerID,
								roomCarMsgDecoder.location));

					case "newCustomer":
						customerMsgDecoder = (CustomerMessageDecoder) decoder;
						customerMsgDecoder.decodeCommandMsgNoCID(content);
						
						
					case "newCustomerID":
						customerMsgDecoder = (CustomerMessageDecoder) decoder;
						customerMsgDecoder.decodeCommandMsg(content);
						
					case "deleteCustomer":

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

				toMW.println(res);
				toMW.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}