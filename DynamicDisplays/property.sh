#!/bin/bash

. ./setupJars.sh

dbFlavor=$(java gov.fnal.ppd.dd.db.DisplayUtilDatabase | grep FLAVOR | awk '{ print $3 }')

if [ "X $dbFlavor" = "X " ]; then
    # The FLAVOR is not in the DB - check the config file

    # Read configuration into an associative array
    declare -A CONFIG
    # IFS is the 'internal field separator'.
    IFS=" "
    while read -r key value
    do
	CONFIG[$key]=$value
    done < config/config.properties
    unset IFS
    
    # If a parameter is passed, look it up by that, else exit
    if [ $1 ]; then
	echo ${CONFIG[$1]}
    fi
    
    exit $?
else
    echo $dbFlavor
fi
