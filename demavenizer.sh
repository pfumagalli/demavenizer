#!/bin/sh

BASEDIR="`dirname $0`"
java -cp "${BASEDIR}/target/eclipse" com.github.pfumagalli.demavenizer.Main "${@}"
