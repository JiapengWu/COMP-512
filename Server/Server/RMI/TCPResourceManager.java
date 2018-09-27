package Server.RMI;

import Server.Interface.*;
import Server.Common.*;


import java.util.*;
import Util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;

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
		
		// setup server
		TCPResourceManager server = new TCPResourceManager();
		server.setRM(s_serverName);

		ServerSocket serverSocket = new ServerSocket(port); // the server socket listens on this port
	    while (true){
	    	Socket mw_socket = serverSocket.accept(); // FIXME: does this socket connec to middleware?
	    	//upon receive msg from middleware, run a new thread
	    	(new TCPServerThread(mw_socket,s_serverName, server.resourceManager)).run(); 
	    } 

		
	}


	public TCPResourceManager(){}

	public void setRM(String name){
		this.resourceManager = new ResourceManager(name);
	}
}
