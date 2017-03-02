#!/bin/bash

BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/game-wrapper-1.2.1.jar "$(cat test/wrapper-commands.json)"
