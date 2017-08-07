#!/bin/bash

BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/match-wrapper-*.jar "$(cat test/scenariowrapper-commands.json)"
echo "${?}"
