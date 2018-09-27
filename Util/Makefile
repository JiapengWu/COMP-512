all: java.policy compile-util

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

compile-util: *.java 
	javac -cp ~/workspace/COMP512/java-json.jar *.java 

clean:
	rm -f *.class

