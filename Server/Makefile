all: java.policy compile-lock-manager compile-server-rmi 

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java LockManager.jar
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

LockManager.jar: Server/LockManager/*.java
	@echo "Compiling LockManagers"
	javac Server/LockManager/*.java
	jar cvf LockManager.jar Server/LockManager/*.class

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java

compile-lock-manager: Server/LockManager/*.java
	javac Server/LockManager/*.java
	jar cvf DeadLockException.jar Server/LockManager/*.class

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/LockManager/*.class
	rm -f RMIInterface.jar
	rm -f java.policy
