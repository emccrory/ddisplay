#!/bin/bash

. setupJars.sh

dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
C=0
while [ $C -lt 30 ]; do
    if ping -c 1 $dbs 
    then
	C=30
    else
	echo Waiting for DB server $dbs to be visible
	sleep 5;
    fi
    let C=C+1;
done
