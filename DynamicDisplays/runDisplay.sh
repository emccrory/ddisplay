#!/bin/bash

cd ~/src/roc-dynamicdisplays/DynamicDisplays

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
/usr/bin/xterm &

. setupJars.sh

d=`date +%F`

log=../../log/display_${d}_$$.log

screenNum=0
if [ "$1 X" != " X" ]; then
    Screennum=$1;
fi
{
    date
# Is this node the messaging server??
    if [ $messagingServer = `uname -n` ]; then
	if java -Dddisplay.messagingserver=$messagingServer \
                -Xmx512m gov.fnal.ppd.chat.MessagingServerTest; then
	    echo Messaging server already running;
	else
	    echo Messaging server is not present so we shall start it
	    ./runMessagingServer.sh & 
	    sleep 10;
	fi
    fi
    
    java -Dddisplay.messagingserver=$messagingServer \
	-Dddisplay.dbserver=$databaseServer \
	-Xmx512m gov.fnal.ppd.signage.display.client.DisplayAsConnectionToFireFox -screen=$screenNum 

} 2>&1 > $log
