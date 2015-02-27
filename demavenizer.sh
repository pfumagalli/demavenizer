#!/bin/sh

BASEDIR="`dirname $0`"
java -cp "${BASEDIR}/target/main" com.github.pfumagalli.demavenizer.Main "${@}"
