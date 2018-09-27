package Server.Common;
import Utils.Message;
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


	// NOTE: doesn't handle Customer --- only connect to 1 server
	@Override
	public run(){
		// connection with client
		BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter toClient = new PrintWriter(socket.getOutputStream(),true);

		// connection with Flight/Car/Room Server
		String serverType = msgDecoder.decode_Type(fromClient.readLine());
		String server_host = ""
		if (serverType) server_host = serverType2host.get(serverType);

		Socket server_socket = new Socket(server_host, server_port);
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(server_socket.getInputStream()));
		PrintWriter toServer = new PrintWriter(server_socket.getOutputStream(),true);

		//TODO: actual shit happening here:

	}


	
}