#!/bin/bash

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
cd ~/src/log
/usr/bin/xterm &

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

d=`date +%F`

log=../../log/display_${d}_$$.log

MyName=`uname -n`
WrapperType=NORMAL

# TODO Remove this bit of hard coding.  Put it in the DB or something
if [ $MyName = "roc-w-02.fnal.gov" -o $MyName = "xocnuc01.fnal.gov" -o $MyName = "wh2e-nuc-14.fnal.gov" ]; then
    WrapperType=TICKER;
fi

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
    /bin/date
    # Is this node the messaging server??
    if [ $messagingServer = $MyName ]; then
	if java -Dddisplay.messagingserver=$messagingServer \
                -Xmx512m gov.fnal.ppd.dd.chat.MessagingServerTest; then
	    echo Messaging server already running;
	else
	    echo Messaging server is not present so we shall start it
	    ./runMessagingServer.sh & 
	    sleep 10;
	fi
    fi
    
    java -Dddisplay.messagingserver=$messagingServer \
	-Dddisplay.dbserver=$databaseServer \
	-Dddisplay.wrappertype=$WrapperType \
	-Xmx512m gov.fnal.ppd.dd.display.client.DisplayAsConnectionToFireFox -screen=$screenNum 

} 2>&1 > $log
