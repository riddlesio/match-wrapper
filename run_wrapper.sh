#!/bin/bash

BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/game-wrapper-1.2.2.jar "$(cat test/wrapper-commands.json)"
