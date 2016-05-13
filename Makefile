all: create-bin-dir unjar-deps gamewrapper


create-bin-dir: ; mkdir -p bin
unjar-deps: ; cd bin && find ../lib -name '*.jar' -exec jar xvf {} \; && rm -R META-INF && cd ..
gamewrapper: ; javac -cp ".:lib/*" -d bin/ `find src/ -name '*.java'` && jar cf gamewrapper.jar -C bin .

clean: ; rm -r bin; rm gamewrapper.jar

.PHONY: all gamewrapper
