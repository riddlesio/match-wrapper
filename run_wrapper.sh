#!/bin/bash

BASEDIR=`pwd`
java -jar $BASEDIR/build/libs/match-wrapper-*.jar "$(cat test/wrapper-commands.json)"
echo "${?}"
