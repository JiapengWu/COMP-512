package Server.Common;

import Utils.Message;
import Utils.MessageDecoder;
import java.io.*;
import java.net.Socket;

public class TCPMiddlewareThread implements Runnable{

	private Socket mw_socket;
	private HashMap<String, String> servertType2host; // {<ServerType>: <Hostname>}
	private server_port = 1099; // port of the servers
	private final MessageDecoder msgDecoder; // each message per thread

	public TCPClientHandler(Socket mw_socket, HashMap<String, String> servertType2host){
		this.mw_socket = mw_socket;
		this.servertType2host = servertType2host;
		msgDecoder = new msgDecoder(); 
	}


	
	@Override
	public run(){
		// connection with client
		BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter toClient = new PrintWriter(socket.getOutputStream(),true);

		// connection with Flight/Car/Room Server
		String request = fromClient.readLine();
		String serverType = msgDecoder.decode_Type(request);
		// FIXME: doesn't handle cases with Customer or multiple server --- this only connect to 1 server
		String server_host = ""
		if (serverType) server_host = serverType2host.get(serverType);

		// forward command to corresponding RM and get result from the server
		String result = sendRecvStr(request, server_host, server_port);
		if (result.equals("<IOException>")) Trace.error("IOException from server "+server_host);
		if (result.equals("<IllegalArgumentException>")) Trace.error("IllegalArgumentException from server "+server_host);

		// write the result back to client
		toClient.println(result);
		toClient.flush();
		socket.close();
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