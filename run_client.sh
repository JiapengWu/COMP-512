##usage: ./run_client.sh [<middleware_hostname>] [port]
source env_setup.sh
java -cp  ${JARPATH} main.java.Client.Client.TCPClient $1 $2
