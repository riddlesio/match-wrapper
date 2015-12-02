#!/bin/bash

# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments
BASEDIR=$(dirname $0)
java -cp $BASEDIR/gamewrapper.jar -Xmx2g io.riddles.gamewrapper.GameWrapper ${1+"$@"}
