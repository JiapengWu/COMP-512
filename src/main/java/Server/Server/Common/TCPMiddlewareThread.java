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


	public TCPMiddlewareThread(Socket client_socket, HashMap<String, String> serverType2host){
		this.client_socket = client_socket;
		this.serverType2host = serverType2host;
		msgDecoder = new MessageDecoder(); 
	}


	
	@Override
	public void run(){
		// connection with client
		BufferedReader fromClient = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
		PrintWriter toClient = new PrintWriter(client_socket.getOutputStream(),true);

		// connection with Flight/Car/Room Server
		String request = fromClient.readLine();
		String serverType = msgDecoder.decode_Type(request);
		Trace.info("TCPMiddlewareThread:: recieve and forward commandType '"+serverType+"'...");
		
		// FIXME: doesn't handle cases with Customer or multiple server --- this only connect to 1 server
		String server_host = "";
		if (serverType!="ALL") server_host = serverType2host.get(serverType);
		// else{
		// 	// need to connect to multiple server here
		// }

		// forward command to corresponding RM and get result from the server
		String result = sendRecvStr(request, server_host);
		if (result.equals("<JSONException>")) Trace.error("IOException from server "+server_host);
		if (result.equals("<IllegalArgumentException>")) Trace.error("IllegalArgumentException from server "+server_host);

		// write the result back to client
		toClient.println(result);
		toClient.flush();
		client_socket.close();
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