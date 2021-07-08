#!/bin/bash

if [ "$1" ]; then
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
    prop="${CONFIG[$1]}"
    # for reasons unkonwn to me, the last character is the \r character.  This removal is not ideal - one would really want 
    # to remove the \r character and leave the string unchanged if this fizes itself somehow.  But the echo|sed line that 
    # works does not pass shellcheck.  And I could not get the other way, "${var//sub/for}", thing to work.
    c=${prop%?}
    echo "$c"
fi

exit $?
