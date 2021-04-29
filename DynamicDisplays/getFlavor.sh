#!/bin/bash

. ./setupJars.sh

dbFlavor=$(java gov.fnal.ppd.dd.db.DisplayUtilDatabase | grep FLAVOR | awk '{ print $3 }')

if [ "X $dbFlavor" = "X " ]; then
    ./property.sh UpdateFlavor
else
    echo $dbFlavor
fi
