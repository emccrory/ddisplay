#!/bin/bash

# All the scripts treat the development node special.  Change these as appropriate
export adminEmail="mccrory@fnal.gov"
export adminNode="ad130482.fnal.gov"
export adminWorkspace="/home/mccrory/git-ddisplay"

export CLASSPATH=target/classes/:bin/:lib/mysql-connector-java-5.1.27.jar:lib/slf4j-api-1.5.8.jar:lib/slf4j-log4j12-1.5.8.jar:lib/selenium/selenium-server-standalone-3.141.59.jar

# Backup database server is irrelevant
export databaseServer="nothing.fnal.gov"

# Read the database credentials from the secret file
array=()

getArray() {
    i=0
    while read -r line
    do
	array[i]=$line
	i=$((i + 1))
    done < "$1"
}

CREDENTIALS="$HOME/keystore/credentials.txt"
if [ -e "$CREDENTIALS" ]; then
	getArray "$CREDENTIALS"

	export databaseServer=${array[0]}:${array[1]}
	export databaseName=${array[2]}
	export databaseUsername=${array[3]}
	export databasePassword=${array[4]}

	# echo $databaseServer $databaseUsername $databasePassword 
fi

export PATH=$PATH:/Applications/Firefox.app/Contents/MacOS
