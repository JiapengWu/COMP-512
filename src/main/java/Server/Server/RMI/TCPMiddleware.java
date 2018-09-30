package main.java.Server.Server.RMI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.Server.Server.Common.TCPMiddlewareThread;
import main.java.Server.Server.Common.Trace;

public class TCPMiddleware {
	private static String s_serverName = "TCPMiddleware"; // this is useless cuz we only need the port to start a server socket
  	private static int s_port = 1099; // port of middleware, listen to client
  	private static ArrayList<Integer> customerIdx = new ArrayList<Integer>();

  	public TCPMiddleware(){

  	}

  	public static void main(String[] args){
  		/* 
  		argv[0]: middleware hostname
  		argv[1]: flight server hostname
  		argv[2]: car server hostname
  		argv[3]: room server hostname
  		*/
  		if (args.length ==4) {
	      s_serverName = args[0];
	    }
  		if (args.length ==5) {
  			s_port = Integer.parseInt(args[4]);
  	    }
	    else{
	    	Trace.error("RMIMiddleWare:: Expect 4 arguments. $0: hostname of MiddleWare, $1-$3: hostname of servers");
	    	System.exit(1);
	    }
	    final HashMap<String, String> serverType2host = new HashMap<String, String>();
	    serverType2host.put("Flight",args[1]);
	    serverType2host.put("Car",args[2]);
	    serverType2host.put("Room",args[3]);

	    ServerSocket middlewareServerSocket = null;
		try {
			middlewareServerSocket = new ServerSocket(s_port);
		} catch (IOException e) {
			Trace.error("Failed to initialize middleware socket.");
		}
	    Trace.info("TCPMiddleware:: server '" + s_serverName + "' start listening to clients on port " + Integer.toString(s_port));
	    
	    int errorCounter = 0;
	    while (true){
	    	Socket socket = null;
			try {
				socket = middlewareServerSocket.accept();
				errorCounter = 0;
			} catch (IOException e) {
				errorCounter++;
				if(errorCounter == 10000)
				{
					Trace.error("Middleware cannot receive message. Quitting...");
					break;
				}
			} 

	    	(new TCPMiddlewareThread(socket, serverType2host, customerIdx)).run();
	    	// FIXME: does this socket connec to client?
	    }
	    try {
			middlewareServerSocket.close();
		} catch (IOException e) {
			Trace.error("Failed to close middleware socket.");
		}
  	}
}