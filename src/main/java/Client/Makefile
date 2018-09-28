all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Util/utils.jar: ../Util/Message.java
	make -C ../Util/ utils.jar

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java 
	make -C ../Server/ RMIInterface.jar
	

compile-client: ../Server/RMIInterface.jar ../Util/utils.jar
	javac -cp ../Server/RMIInterface.jar:../Util/utils.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy
