##usage: ./run_client.sh [<middleware_hostname>] [port]

java -cp  ${JARPATH} main.java.Client.Client.TCPClient $1 $2
