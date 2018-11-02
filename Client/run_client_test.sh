# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:../Server/Exceptions.jar:../Server/LockManager.jar:. Client.TestClient $1 $2
