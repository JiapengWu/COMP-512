all: java.policy compile-util

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

compile-util: Util/*.java 
	javac Util/*.java 

# clean:
# 	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class
# 	rm -f RMIInterface.jar
# 	rm -f java.policy 
