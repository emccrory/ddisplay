#!/bin/bash

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
cd ~/src/log

if [ -e /usr/bin/xterm ]; then
    # SLF
    /usr/bin/xterm &
elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
    /opt/X11/bin/xterm &
fi

cd ~/src/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

d=`date +%F`

log=../../log/display_${d}_$$.log

# Do not begin until we can ping the database server

dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
C=0
{
    while [ $C -lt 30 ]; do
	if ping -c 1 $dbs 
	then
	    break
	else
	    echo Waiting for DB server $dbs to be visible
	    sleep 5;
	fi
	let C=C+1;
    done
    if [ $C -ge 30 ]; then
	echo Database server is not reachable.  Goodbye.
	exit
    fi
} 2>&1 >> $log

MyName=`uname -n`
WrapperType=NORMAL

# TODO Remove this bit of hard coding.  Put it in the DB or something
if [ $MyName = "xocnuc01.fnal.gov" -o $MyName = "wh2e-nuc-14.fnal.gov" ]; then
    WrapperType=TICKER;
fi

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
# Get the messaging server for me

    messagingServer=`java -Dddisplay.dbserver=$databaseServer \
			-Dddisplay.dbname=$databaseName \
			-Dddisplay.dbusername=$databaseUsername \
			-Dddisplay.dbpassword=$databasePassword \
			-Xmx512m gov.fnal.ppd.dd.GetMessagingServer | grep "MessagingServer=" | awk '{ print $2 }'`


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
    
    java -Dddisplay.dbserver=$databaseServer \
	-Dddisplay.dbname=$databaseName \
	-Dddisplay.dbusername=$databaseUsername \
	-Dddisplay.dbpassword=$databasePassword \
	-Dddisplay.wrappertype=$WrapperType \
	-Xmx512m gov.fnal.ppd.dd.display.client.DisplayAsConnectionToFireFox -screen=$screenNum 

} 2>&1 >> $log
