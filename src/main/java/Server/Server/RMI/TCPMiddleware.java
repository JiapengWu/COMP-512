package main.java.Server.Server.RMI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import main.java.Server.Server.Common.TCPMiddlewareThread;
import main.java.Server.Server.Common.Trace;

public class TCPMiddleware {
	private static String s_serverName = "TCPMiddleware"; // this is useless cuz we only need the port to start a server socket
  	private static int mw_port = 1099; // port of middleware, listen to client
  	private static int s_port = 1099; // port of server, used for connection and forward
  	private static ArrayList<Integer> customerIdx = new ArrayList<Integer>();

  	public TCPMiddleware(){

  	}

  	public static void main(String[] args){
  		/* 
  		args[0]: middleware hostname
  		args[1]: flight server hostname
  		args[2]: car server hostname
  		args[3]: room server hostname
  		args[4]: middleware port (optional)
  		args[5]: server port (optional)
  		*/
  		if (args.length<4 || args.length>6){
	    	Trace.error("RMIMiddleWare:: 4 required + 2 optional arguments . $0: name of MiddleWare, $1-$3: hostname of servers, optional $4: middleware port, option $5: server port");
	    	System.exit(1);
	    }
  		
  		if (args.length >=5) {
  			mw_port = Integer.parseInt(args[4]);
  	    }
  	    if (args.length >=6) {
  			s_port = Integer.parseInt(args[5]);
  	    }
  	    s_serverName = args[0];
  	   
	    final HashMap<String, String> serverType2host = new HashMap<String, String>();
	    serverType2host.put("Flight",args[1]);
	    serverType2host.put("Car",args[2]);
	    serverType2host.put("Room",args[3]);

	    ServerSocket middlewareServerSocket = null;
		try {
			middlewareServerSocket = new ServerSocket(mw_port);
		} catch (IOException e) {
			Trace.error("Failed to initialize middleware socket.");
		}
	    Trace.info("TCPMiddleware:: server '" + s_serverName + "' start listening to clients on port " + Integer.toString(mw_port));
	    for (Map.Entry<String, String> entry : serverType2host.entrySet()){
			Trace.info("serverType2host: "+entry.getKey()+" -> "+entry.getValue());	
		}

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

	    	(new TCPMiddlewareThread(socket, serverType2host, customerIdx, s_port)).run();
	    	// FIXME: does this socket connec to client?
	    }
	    try {
			middlewareServerSocket.close();
		} catch (IOException e) {
			Trace.error("Failed to close middleware socket.");
		}
  	}
}