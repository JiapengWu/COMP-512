package Server.RMI;

import Server.Common.TCPMiddlewareThread;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import Server.Common.Trace;
import java.util.*;

public class TCPMiddleware {
	private static String s_serverName = "TCPMiddleware"; // this is useless cuz we only need the port to start a server socket
  	private static int s_port = 1099; // port of middleware, listen to client
  	
  	public TCPMiddleware(){

  	}

  	public static void main(String[] args) throws IOException{
  		/* 
  		argv[0]: middleware hostname
  		argv[1]: flight server hostname
  		argv[2]: car server hostname
  		argv[3]: room server hostname
  		*/
  		if (args.length ==4) {
	      s_serverName = args[0];
	    }
	    else{
	    	Trace.error("RMIMiddleWare:: Expect 4 arguments. $0: hostname of MiddleWare, $1-$3: hostname of servers");
	    	System.exit(1);
	    }

	    HashMap<String, String> serverType2host = new HashMap<String, String>();
	    serverType2host.put("Flight",args[1]);
	    serverType2host.put("Car",args[2]);
	    serverType2host.put("Room",args[3]);

	    ServerSocket serverSocket = new ServerSocket(s_port);
	    Trace.info("TCPMiddleware:: server '"+s_serverName+"' start listening to clients on port "+Integer.toString(s_port));
	    while (true){
	    	Socket socket = serverSocket.accept(); // FIXME: does this socket connec to client?
	    	(new TCPMiddlewareThread(socket, serverType2host)).run();
	    }

  	}

}