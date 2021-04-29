#!/bin/bash

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
