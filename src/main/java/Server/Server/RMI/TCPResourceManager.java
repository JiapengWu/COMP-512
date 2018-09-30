package main.java.Server.Server.RMI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import main.java.Server.Server.Common.ResourceManager;
import main.java.Server.Server.Common.TCPServerThread;
import main.java.Server.Server.Common.Trace;

public class TCPResourceManager {
	private static String s_serverName = "Server";
	private static int port = 1099; // server port
	protected ResourceManager resourceManager;

	/*
	Run TCP server:
		args[0]: name of the server
		args[1]: port of the server (optional)
		
	*/
	public static void main(String args[]) 
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}
		if (args.length>1) port=Integer.parseInt(args[1]);

		// setup server
		TCPResourceManager server = new TCPResourceManager();
		server.setRM(s_serverName);

		ServerSocket serverSocket = null;
	    try {
	    	serverSocket = new ServerSocket(port);
	    } catch (IOException e) {
			e.printStackTrace();
			Trace.error("Failed to initialize server socket");
			System.exit(0);
	    } // the server socket listens on this port

	    Trace.info("TCPResourceManager:: Connected to server '"+s_serverName+"' with port "+Integer.toString(port));
	    int counter = 0;
	    while (true){
			Socket mw_socket = null;
			try {
				mw_socket = serverSocket.accept();
				counter = 0;
			} catch (IOException e) {
				counter += 1;
			if(counter == 10000) {
				break;
				}
			} 
			(new TCPServerThread(mw_socket,s_serverName, server.resourceManager)).run(); 
			// input socket is the middleware socket
    	//upon receive msg from middleware, run a new thread
    } 
		try {
		  serverSocket.close();
		} catch (IOException e) {
		  Trace.warn("Failed to close the server socket.");;
		}
	}

	public void setRM(String name){
		this.resourceManager = new ResourceManager(name);
	}
}
