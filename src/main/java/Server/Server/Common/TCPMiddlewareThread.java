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
import main.java.Util.MessageDecoder.BundleMessageDecoder;
import main.java.Util.MessageDecoder.CustomerMessageDecoder;

public class TCPMiddlewareThread implements Runnable{

	private Socket client_socket;
	private HashMap<String, String> serverType2host; // {<ServerType>: <Hostname>}
	private int server_port = 1099; // port of the servers
	private ArrayList<Integer> customerIdx;


	public TCPMiddlewareThread(Socket client_socket, HashMap<String, String> serverType2host, ArrayList<Integer> customerIdx, int server_port){
		this.client_socket = client_socket;
		this.serverType2host = serverType2host;
		this.customerIdx=customerIdx;
		this.server_port = server_port;
	}
	
	@Override
	public void run(){
		Trace.info("TCPMiddlewareThread:: new request received");
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
		Trace.info("Receive command "+command+", forward to '"+serverType+"' server");
		
		String result = "";
		String server_host = "";

		if (!serverType.equals("ALL")){ // forward command to corresponding RM and get result from the server
			server_host = serverType2host.get(serverType);
			result = sendRecvStr(request, server_host);
			if (result.equals("<JSONException>")) Trace.error("IOException from server "+server_host);
			if (result.equals("<IllegalArgumentException>")) Trace.error("IllegalArgumentException from server "+server_host);
		}
		
		else if (!command.equals("bundle")){ // customer-related command
			CustomerMessageDecoder customerMsgDecoder = (new MessageDecoder()).new CustomerMessageDecoder();
			
			switch (command){
				case "newCustomer":
					customerMsgDecoder.decodeCommandMsgNoCID(content);
					synchronized (customerIdx){
						int cid;
						if (customerIdx.size()==0) cid=1;
						else cid = Collections.max(customerIdx)+1;
						customerIdx.add(cid);
						sendCustomerCommand(command, customerMsgDecoder.id, cid);
						result = Integer.toString(cid);
					break;
					}
				case "newCustomerID":
					customerMsgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand("newCustomer", customerMsgDecoder.id, customerMsgDecoder.customerID);
					break;
				case "queryCustomerInfo":
					customerMsgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand(command, customerMsgDecoder.id, customerMsgDecoder.customerID);
					break;
				case "deleteCustomer":
					customerMsgDecoder.decodeCommandMsg(content);
					synchronized(customerIdx){
			      		customerIdx.remove((Integer) customerMsgDecoder.customerID);
			    	}
					result = sendCustomerCommand(command, customerMsgDecoder.id, customerMsgDecoder.customerID);
					break;
				default:
					result = "<IllegalArgumentException>";
					throw new IllegalArgumentException(content);
			}

		}
		else if (command.equals("bundle")){
			BundleMessageDecoder bundleMsgDecoder = (new MessageDecoder()).new BundleMessageDecoder();
			try {
				sendBundleCommand(content, bundleMsgDecoder);
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
	public String sendBundleCommand(String content, BundleMessageDecoder bundleMsgDecoder) throws IllegalArgumentException, IOException{
		// TODO: parse {int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room}
		bundleMsgDecoder.decodeCommandMsg(content);
		int id = bundleMsgDecoder.id;
		int cid = bundleMsgDecoder.customerID;
		Vector<String> flightNumbers = bundleMsgDecoder.flightNums;
		String location = bundleMsgDecoder.location;
		boolean car = bundleMsgDecoder.car;
		boolean room = bundleMsgDecoder.room;
		
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
		if (room){
			Message msg = new Message("reserveRoom");
			msg.reserveCommand(id, cid, location);
			result = sendRecvStr(msg.toString(),roomServer);
			res = res && Boolean.parseBoolean(result);
		}
		if (car){
			Message msg = new Message("reserveCar");
			msg.reserveCommand(id, cid, location);
			result = sendRecvStr(msg.toString(),carServer);
			res = res && Boolean.parseBoolean(result);
		}
		return result;
	}

	// send a command that involves customer to a server
	public String sendCustomerCommand(String cmd, int id, int cid){
		Message msg = new Message(cmd);
		msg.addDeleteQueryCustomerCommand(id,cid);
		
		boolean res = true;
		for (Map.Entry<String, String> entry : serverType2host.entrySet()){
			Trace.info("TCPMiddlewareThread:: send command '"+cmd+"' to server "+entry.getKey()+" with hostname '"+entry.getValue()+"'");
			String result = "";
			result = sendRecvStr(msg.toString(), entry.getValue());

			if (result.equals("<JSONException>")) {Trace.error("IOException from server "+entry.getValue()); return result;}
			if (result.equals("<IllegalArgumentException>")) {Trace.error("IllegalArgumentException from server "+entry.getValue());return result;}
			res = res && Boolean.parseBoolean(result);
		}
		return Boolean.toString(res);
	}


	public String sendRecvStr(String request, String server_host){
		// connect to the server
		Socket server_socket = null;
		PrintWriter toServer = null;
		BufferedReader fromServer = null;
		try {
			server_socket = new Socket(server_host, server_port);
			fromServer = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
			toServer = new PrintWriter(server_socket.getOutputStream(),true);
		} catch (Exception e) {
			Trace.error("TCPMiddlewareThread:: Cannot open socket @"+server_host+":"+Integer.toString(server_port));
			e.printStackTrace();
		}
		// write to server
		toServer.println(request.toString());
		toServer.flush();
		
		
		
		String res = "";
		try {
			res = fromServer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (res.equals("IOException")){Trace.error("Middleware get IOException");}
		if (res.equals("IllegalArgumentException")){Trace.error("Middleware get IllegalArgumentException");}

		try {
			server_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	
	
	
	
}