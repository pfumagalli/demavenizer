#!/bin/sh

java -cp target/eclipse \
     -Dmaven.url=http://repo1.maven.org/maven2/ \
     -Dlibraries.dir=./libraries \
     -Dmappings.file=./libraries/mappings.properties \
     -Dlicenses.file=./libraries/licenses.properties \
     com.github.pfumagalli.demavenizer.Main "${@}"
