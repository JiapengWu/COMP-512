package main.java.Server.Server.Common;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import main.java.Util.Message;
import main.java.Util.MessageDecoder;
import main.java.Util.MessageDecoder.CustomerMessageDecoder;

public class TCPMiddlewareThread implements Runnable{

	private Socket client_socket;
	private HashMap<String, String> serverType2host; // {<ServerType>: <Hostname>}
	private int server_port = 1099; // port of the servers
	private ArrayList<Integer> customerIdx;


	public TCPMiddlewareThread(Socket client_socket, HashMap<String, String> serverType2host, ArrayList<Integer> customerIdx ){
		this.client_socket = client_socket;
		this.serverType2host = serverType2host;
		this.customerIdx=customerIdx;
	}
	
	@Override
	public void run(){
		// connection with client
		BufferedReader fromClient = null;
		PrintWriter toClient = null;
		try {
			toClient = new PrintWriter(client_socket.getOutputStream(),true);
			fromClient = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
		} catch (IOException e) {
			Trace.error("Failed to start buffer reader and writer.");
			e.printStackTrace();
		}

		// parse request into: server_to_forward; command name; command arguments
		String request = null;
		try {
			request = fromClient.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String serverType = MessageDecoder.getServerType(request);
		String command = MessageDecoder.getCommand(request);
		String content = MessageDecoder.getContent(request);
		Trace.info("TCPMiddlewareThread:: recieve and forward commandType '" + serverType + "'...");
		
		String result = "";
		String server_host = "";
		if (serverType != "ALL"){
			server_host = serverType2host.get(serverType);
			// forward command to corresponding RM and get result from the server
			try {
				result = sendRecvStr(request, server_host);
			} catch (IllegalArgumentException | IOException e) {
				e.printStackTrace();
			}
			if (result.equals("<JSONException>")) Trace.error("IOException from server "+server_host);
			if (result.equals("<IllegalArgumentException>")) Trace.error("IllegalArgumentException from server "+server_host);
		}
		
		else if (command!="bundle"){
			// customer-related command
			MessageDecoder msgDecoder = new MessageDecoder();
			CustomerMessageDecoder customerMsgDecoder = msgDecoder.new CustomerMessageDecoder();

			switch (command){
				case "newCustomer":
					customerMsgDecoder.decodeCommandMsgNoCID(content);
					synchronized (customerIdx){
						int cid = Collections.max(customerIdx)+1;
						customerIdx.add(cid);
						sendCustomerCommand(command, customerMsgDecoder.id, cid);
						result = Integer.toString(cid);
					}
				case "newCustomerID":
					customerMsgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand("newCustomer", customerMsgDecoder.id, customerMsgDecoder.customerID);

				case "queryCustomerInfo":
					customerMsgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand(command, customerMsgDecoder.id, customerMsgDecoder.customerID);

				case "deleteCustomer":
					customerMsgDecoder.decodeCommandMsg(content);
					synchronized(customerIdx){
			      		customerIdx.remove(customerMsgDecoder.customerID);
			    	}
					result = sendCustomerCommand(command, customerMsgDecoder.id, customerMsgDecoder.customerID);

				case "addFlight":
					customerMsgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand(command, customerMsgDecoder.id, customerMsgDecoder.customerID);

					
				default:
					result = "<IllegalArgumentException>";
					throw new IllegalArgumentException(content);
			}

		}
		else if (command=="bundle"){
			try {
				sendBundleCommand(content);
			} catch (IllegalArgumentException | IOException e) {
				Trace.error("Failed to sent bundle command");
				e.printStackTrace();
			}
		}

		// write the result back to client
		toClient.println(result);
		toClient.flush();
		try {
			client_socket.close();
		} catch (IOException e) {
			Trace.error("Failed to close client socket.");
			e.printStackTrace();
		}
	}


	// handle case "bundle"	
	/*
	public String sendBundleCommand(String content) throws IllegalArgumentException, IOException{
		// TODO: parse {int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room}
		Vector<String> flightNumbers = new Vector<String>();
		int id, cid;
		String location, car, rooms;

		boolean res = true;
		String result = "";
		String flightServer = serverType2host.get("Flight");
		String roomServer = serverType2host.get("Room");
		String carServer = serverType2host.get("Car");
		for (String fn: flightNumbers){
			Message msg = new Message("reserveFlight");
			msg.reserveFlightCommand(id, cid, Integer.parseInt(fn));
			result = sendRecvStr(msg.toString(), flightServer);
			res = res && Boolean.parseBoolean(result);
		}
		if (rooms == "true"){
			Message msg = new Message("reserveRoom");
			msg.reserveCommand(id,cid, location);
			result = sendRecvStr(msg.toString(),roomServer);
			res = res && Boolean.parseBoolean(result);
		}
		if (car == "true"){
			Message msg = new Message("reserveCar");
			msg.reserveCommand(id,cid, location);
			result = sendRecvStr(msg.toString(),carServer);
			res = res && Boolean.parseBoolean(result);
		}
		return result;

	}
	*/

	// send a command that involves customer to a server
	public String sendCustomerCommand(String cmd, int id, int cid){
		Message msg = new Message(cmd);
		msg.addDeleteQueryCustomerCommand(id,cid);
		boolean res = true;
		for (Map.Entry<String, String> entry : serverType2host.entrySet()){
			Trace.info("TCPMiddlewareThread:: send command '"+cmd+"' to server "+entry.getKey()+" with hostname '"+entry.getValue()+"'");
			String result = "";
			try {
				result = sendRecvStr(msg.toString(), entry.getValue());
			} catch (IllegalArgumentException | IOException e) {
				Trace.error("Failed to send customer command to server.");
				e.printStackTrace();
			}
			if (result.equals("<JSONException>")) {Trace.error("IOException from server "+entry.getValue()); return result;}
			if (result.equals("<IllegalArgumentException>")) {Trace.error("IllegalArgumentException from server "+entry.getValue());return result;}
			res = res && Boolean.parseBoolean(result);
		}
		return Boolean.toString(res);
	}


	public String sendRecvStr(String request, String server_host) throws IOException{
		// connect to the server
		Socket server_socket = null;
		PrintWriter toServer = null;
		BufferedReader fromServer = null;
		try {
			server_socket = new Socket(server_host, server_port);
			fromServer = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
			toServer = new PrintWriter(server_socket.getOutputStream(),true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// write to server
		toServer.println(request);
		toServer.flush();
		
		try {
			server_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String res = null;
		try {
			res = fromServer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (res.equals("<IOException>")) throw new IOException();
		if (res.equals("<IllegalArgumentException>")) throw new IllegalArgumentException();
		return res;
	}
	
	
	
}