JAR := ../lib/json.jar
CLASSPATH := .:$(JAR)

build:
	javac -cp "$(CLASSPATH)" *.java

run:
	java -cp "$(CLASSPATH)" Main $(ARGS)

clean:
	rm -f *.class
