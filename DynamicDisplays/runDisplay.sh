#!/bin/bash

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
cd ~/src/log

if [ -e /usr/bin/xterm ]; then
    # SLF
    /usr/bin/xterm  -geometry 200x30 -fa 'Monospace' -fs 12 &
elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
    /opt/X11/bin/xterm -e 'echo "Starting X11"'&
fi

cd ~/src/roc-dynamicdisplays/DynamicDisplays

./runVersionInformation.sh Y

. setupJars.sh

d=`date +%F`

log=../../log/display_${d}_$$.log

# Do not begin until we can ping the database server

dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
sleepTime=5
{
    while :
    do
    # Forever loop. The DB server HAS TO appear eventually!
	if ping -c 1 $dbs 
	then
	    break
	else
	    echo `date` Waiting for DB server $dbs to be visible $sleepTime
	    sleep $sleepTime;
	fi
	let sleepTime=sleepTime+1;
	if [ $sleepTime -gt 300 ]; then
	    sleepTime=300;
	fi
    done
} >> $log 2>&1

MyName=`uname -n`
# WrapperType=NORMAL
WrapperType=FRAMENOTICKER # 3

# TODO Remove this bit of hard coding.  Put it in the DB or something
if [ $MyName = "xocnuc01.fnal.gov" -o $MyName = "wh2e-nuc-14.fnal.gov" ]; then
    WrapperType=TICKER; # 1
fi

if [ $MyName = "roc-w-01.fnal.gov" -o $MyName = "mccrory.fnal.gov" ]; then
    WrapperType=FERMITICKER; # 5
fi

if [ $MyName = "adnetdisplay1-mac.fnal.gov" ]; then
    WrapperType=FRAMENOTICKER; # 3
fi

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
    echo "Obtaining my messaging server, and determining if it is this node ... "
    # Get the messaging server for me
    messagingServer=`java -Xmx512m gov.fnal.ppd.dd.GetMessagingServer | grep "MessagingServer=" | awk '{ print $2 }'`

    /bin/date
    # Am I the messaging server??
    if [ $messagingServer = $MyName ]; then
	if java -Dddisplay.messagingserver=$messagingServer \
                -Xmx512m gov.fnal.ppd.dd.chat.MessagingServerTest; then
	    echo Messaging server already running;
	else
	    echo Messaging server is not present so we shall start it
	    ./runMessagingServer.sh & 
	    sleep 10;
	fi
    else
	echo "The messaging server is " $messagingServer ", which is not this node"
    fi
    
    echo "Determining if I should run a ChannelSelector"
    if java gov.fnal.ppd.dd.util.HasChannelSelector; then
	if ps -aef | grep MakeChannelSelector | grep -v grep; then
	    echo "Already running the ChannelSelector."
	else
	    echo "Starting the ChannelSelector";
	    ./runSelector.sh SKIP
	    sleep 10;
	fi
    fi

    echo "Running the display software ..."
    java -Dddisplay.wrappertype=$WrapperType -Xmx1024m \
         gov.fnal.ppd.dd.display.client.DisplayAsConnectionToFireFox -screen=$screenNum 

} >> $log 2>&1 
