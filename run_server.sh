##usage: ./run_server.sh [<rmi_name>] [port_name]

java -cp ${JARPATH} main.java.Server.Server.RMI.TCPResourceManager $1 $2
