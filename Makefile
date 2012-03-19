build:
	cd src; javac -cp ../lib/java-cup-11a-runtime.jar *.java; mv *.class ../bin; cd ..
clean:
	rm bin/*.class
