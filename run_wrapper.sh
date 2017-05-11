#!/bin/bash

BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/game-wrapper-*.jar "$(cat test/scenariowrapper-commands.json)"
echo "${?}"
