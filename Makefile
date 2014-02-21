JFLAGS = -g
JC = javac -extdirs lib/ -classpath src/
.SUFFIXES: .java .class .jar
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	src/Query.java \
	src/Doc.java \
	src/RetrievalIteration.java \
	src/Driver.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) src/*.class
