JFLAGS = -g
JC = javac
#
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java
CLASSES = \
	Chunk.class \
	DownloadingFile.class \
	fileStruct.class \
	MulticastP2P.class \
	UploadingFile.class \
	MP2P.class
default: classes
classes: $(CLASSES:.java=.class)
clean:
	$(RM) *.class