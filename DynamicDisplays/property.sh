#!/bin/bash

if [ $1 ]; then
    # Read configuration into an associative array
    declare -A CONFIG
    # IFS is the 'internal field separator'.
    IFS=" "
    while read -r key value
    do
	CONFIG[$key]=$value
    done < config/config.properties
    unset IFS
    
    # Look up the parameters as passed as the first argument here
    echo ${CONFIG[$1]} | sed 's/\r//g' # It always seems to end in \r
fi

exit $?
