package Server.RMI;

import Server.Common.TCPMiddlewareThread;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import Server.Common.Trace;
import java.util.*;

public class TCPMiddleWare {
	private static String s_serverHost = "localhost"; // this is useless cuz we only need the port to start a server socket
  	private static int s_port = 1099; // port of middleware
  	
  	public TCPMiddleWare(){

  	}

  	public static void main(String[] args) throws IOException{
  		/* 
  		argv[0]: middleware hostname
  		argv[1]: flight server hostname
  		argv[2]: car server hostname
  		argv[3]: room server hostname
  		*/
  		if (args.length ==4) {
	      s_serverHost = args[0];
	    }
	    else{
	    	Trace.error("RMIMiddleWare:: Expect 4 arguments. $0: hostname of MiddleWare, $1-$3: hostname of servers");
	    	System.exit(1);
	    }

	    HashMap<String, String> servertType2host = new HashMap<String, String>();
	    servertType2host.put("Flight",args[1]);
	    servertType2host.put("Car",args[2]);
	    servertType2host.put("Room",args[3]);

	    ServerSocket serverSocket = new ServerSocket(s_port);
	    while (true){
	    	Socket socket = serverSocket.accept();
	    	(new TCPMiddlewareThread(socket, serverType2host)).run();
	    }

  	}

}