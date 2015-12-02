all: gamewrapper

gamewrapper: ; mkdir -p bin && javac -d bin/ `find src/ -name '*.java'` && jar cf gamewrapper.jar -C bin .

clean: ; rm -r bin; rm gamewrapper.jar

.PHONY: all gamewrapper
