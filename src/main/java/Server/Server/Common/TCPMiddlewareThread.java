package main.java.Server.Server.Common;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import main.java.Util.MessageDecoder;

public class TCPMiddlewareThread implements Runnable{

	private Socket client_socket;
	private HashMap<String, String> serverType2host; // {<ServerType>: <Hostname>}
	private int server_port = 1099; // port of the servers
	private final MessageDecoder msgDecoder; // each message per thread
	private ArrayList<Integer> customerIdx ;


	public TCPMiddlewareThread(Socket client_socket, HashMap<String, String> serverType2host,ArrayList<Integer> customerIdx ){
		this.client_socket = client_socket;
		this.serverType2host = serverType2host;
		msgDecoder = new MessageDecoder(); 
		this.customerIdx=customerIdx;
	}


	
	@Override
	public void run(){
		// connection with client
		BufferedReader fromClient = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
		PrintWriter toClient = new PrintWriter(client_socket.getOutputStream(),true);

		// parse request into: server_to_forward; command name; command arguments
		String request = fromClient.readLine();
		String serverType = msgDecoder.decodeType(request);
		String command = msgDecoder.decodeCommand(request);
		String content = msgDecoder.getContent(request);
		Trace.info("TCPMiddlewareThread:: recieve and forward commandType '"+serverType+"'...");
		
		
		String server_host = "";
		if (serverType!="ALL"){
			server_host = serverType2host.get(serverType);
			// forward command to corresponding RM and get result from the server
			String result = sendRecvStr(request, server_host);
			if (result.equals("<JSONException>")) Trace.error("IOException from server "+server_host);
			if (result.equals("<IllegalArgumentException>")) Trace.error("IllegalArgumentException from server "+server_host);
		}
		else if (command!="bundle"){
			// customer-related command
			
			CustomerMessageDecoder msgDecoder = (CustomerMessageDecoder) msgDecoder;
			

			switch (command){
				case "newCustomer":
					decodeCommandMsgNoCID(content);
					synchronized (customerIdx){
						int cid = Collections.max(customerIdx)+1;
						customerIdx.put(cid);
					}
					sendCustomerCommand(command, id,cid);
					result = Integer.toString(cid);
				case "newCustomerID":
					msgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand("newCustomer", msgDecoder.id, msgDecoder.cid);

				case "queryCustomerInfo":
					msgDecoder.decodeCommandMsg(content);
					result = sendCustomerCommand(command, msgDecoder.id, msgDecoder.cid);

				case "deleteCustomer":
					msgDecoder.decodeCommandMsg(content);
					synchronized(customerIdx){
			      		customerID.remove(cid);
			    	}
					result = sendCustomerCommand(command, msgDecoder.id, msgDecoder.cid);

				default:
					result = "<IllegalArgumentException>";
					throw new IllegalArgumentException(content);
			}

		}
		else if (commmand="bundle"){
			sendBundleCommand(content);
		}

		// write the result back to client
		toClient.println(result);
		toClient.flush();
		client_socket.close();
	}


	// handle case "bundle"	
	public String sendBundleCommand(String content){
		// TODO: parse {int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room}
		Vector<String> flightNumbers;
		int id;cid;
		String location;car;rooms;

		boolean res = true;

		String flightServer = serverType2host.get("Flight");
		String roomServer = serverType2host.get("Room");
		String carServer = serverType2host.get("Car");
		for (String fn: flightNumbers){
			Message msg = new Message("reserveFlight");
			msg.reserveFlightCommand(id,cid, fn);
			String result = sendRecvStr(msg.toString(),flightServer);
			res = res && Boolean.parseBoolean(result);
		}
		if (room=="true"){
			Message msg = new Message("reserveRoom");
			msg.reserveCommand(id,cid, location);
			String result = sendRecvStr(msg.toString(),roomServer);
			res = res && Boolean.parseBoolean(result);
		}
		if (car=="true"){
			Message msg = new Message("reserveRoom");
			msg.reserveCommand(id,cid, location);
			String result = sendRecvStr(msg.toString(),carServer);
			res = res && Boolean.parseBoolean(result);
		}
		return result;

	}

	// send a command that involves customer to a server
	public String sendCustomerCommand(String cmd, int id, int cid){
		Message msg = new Message(cmd);
		msg.addCustomerCommand(id,cid);
		boolean res = true;
		for (Map.Entry<<String, String>> entry : serverType2host.entrySet()){
			Trace.info("TCPMiddlewareThread:: send command '"+cmd+"' to server "+entry.getKey()+" with hostname '"+entry.getValue()+"'");
			String result = sendRecvStr(msg.toString(), entry.getValue);
			if (result.equals("<JSONException>")) {Trace.error("IOException from server "+server_host); return result;}
			if (result.equals("<IllegalArgumentException>")) {Trace.error("IllegalArgumentException from server "+server_host);return result;}
			res = res && Boolean.parseBoolean(result);
		}
		return Boolean.toString(res);
	}


	public String sendRecvStr(String request, String server_host) throws IOException, IllegalArgumentException{
		Socket server_socket = new Socket(server_host, server_port);
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
		PrintWriter toServer = new PrintWriter(server_socket.getOutputStream(),true);

		toServer.println(request);
		toServer.flush();
		String res = fromServer.readLine();
		if (res.equals("<IOException>")) throw new IOException();
		if (res.equals("<IllegalArgumentException>")) throw new IllegalArgumentException();
		return res;
	}

	// FIXME: Implement IResourceManager Interface here??? 

	
}