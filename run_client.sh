##usage: ./run_client.sh [<middleware_hostname>] [port]

cd ~/COMP-512/target
java -cp COMP-512-Project1-0.0.1-SNAPSHOT-jar-with-dependencies.jar  main.java.Client.Client.TCPClient $1 $2
