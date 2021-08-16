#!/bin/bash
#
# Shell script to check that the hostname from the JVM matches the hostname from the system
#

. setupEnvironment.sh

CLIENTNAME=$(hostname | sed 's/.fnal.gov//g' | sed 's/.local//g' )

echo "The system name from the operating system for this computer is $CLIENTNAME"
JVM_NAME=$(java gov.fnal.ppd.security.CheckHostName)
echo "$JVM_NAME"

n=$(echo "$JVM_NAME" | awk '{ print $9 }' | sed "s/'//g")

if [ "$n" = "$CLIENTNAME" ]; then
    echo These names match
else
    echo "These names DO NOT MATCH: JVM name is '$n', Operating system name is '$CLIENTNAME'"
    exit 1;
fi
