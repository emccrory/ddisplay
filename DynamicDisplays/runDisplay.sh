#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
/usr/bin/xterm &

. setupJars.sh

d=`date +%F`

log=../log/display_${d}_$$.log

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi

java gov.fnal.ppd.signage.display.testing.DisplayAsConnectionToFireFox -screen=$screenNum 2>&1 > $log
