##usage: ./run_server.sh [<rmi_name>] [port_name]

cd ~/workspace/COMP512/target
java -cp COMP-512-Project1-0.0.1-SNAPSHOT.jar main.java.Server.Server.RMI.TCPResourceManager $1 $2