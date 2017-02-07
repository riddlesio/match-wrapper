#!/bin/bash

# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments
BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/game-wrapper-1.0.4.jar ${1+"$@"}
