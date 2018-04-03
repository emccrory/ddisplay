#!/bin/bash

d=`date +%F`

# Set up log file and executables locations
log=~/src/log/display_${d}_$$.log
workingDirectory=~/src/roc-dynamicdisplays/DynamicDisplays
touch $log

# Verify that this script is not running now
if ps -aef | grep $workingDirectory/$0 | grep -v grep ; then
    echo `date` It looks like this script is already running > $log
    exit 1;
fi

# Test for and remove the cache file from disk
cd ~/.mozilla/firefox/*.default
if [ -e places.sqlite ]; then
    ls -l  places.sqlite >> $log 2>&1
    echo Removing disk-based history/cache >> $log 2>&1
    rm -fv places.sqlite >> $log 2>&1
fi

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
cd ~/src/log

if [ -e /usr/bin/xterm ]; then
    # SLF 6.x
    /usr/bin/xterm -geometry 200x30 -fa 'Monospace' -fs 12 &
elif [ -e /usr/bin/gnome-terminal ]; then
    # SL 7.x
    /usr/bin/gnome-terminal --geometry 200x30 --zoom=1.5 &
elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
    # First, wait for the X Server to initialize
    /opt/X11/bin/xterm -e 'echo "Starting X11"'
    # Now start up an X terminal so we can use it to look at log files and stuff
    /opt/X11/bin/xterm -geometry 200x30 &
fi

# Remove the json file that seems to be responsible for configuring Firefox.
# In particular, this holds the last location of the Firefox windows.
# But this does not seem to have the desired effect (3/2018) - more work needed.
# ls -l ~/.mozilla/firefox/*Dynamic*/*.json >> $log 2>&1
# echo Removing xulstore.json files >> $log 2>&1
# rm -fv ~/.mozilla/firefox/*Dynamic*/xulstore.json >> $log 2>&1

cd $workingDirectory

echo `date` `pwd` >> $log

# Check the version of the code
./runVersionInformation.sh Y >> $log 2>&1

cd $workingDirectory

# Prepare to run the Java applications
. setupJars.sh

# Do not begin until we can ping the database server

dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
sleepTime=5
{
    while :
    do
    # Forever loop. We assume that the DB server will appear eventually
	if ping -c 1 $dbs 
	then
	    break
	else
	    echo `date` Waiting $sleepTime seconds for the DB server $dbs to be visible 
	    sleep $sleepTime;
	fi
	let sleepTime=sleepTime+2;
	if [ $sleepTime -gt 150 ]; then
	    sleepTime=150; # It looks like we are going to be here a while.  Recheck every 2.5 minutes.
	fi
    done
} >> $log 2>&1

MyName=`uname -n`
# WrapperType=NORMAL
WrapperType=FRAMENOTICKER # 3

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
    echo `date` "Obtaining my messaging server, and determining if it is this node ... "
    # Get the messaging server for me
    messagingServer=`java -Xmx512m gov.fnal.ppd.dd.GetMessagingServer | grep "MessagingServer=" | awk '{ print $2 }'`

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
    
    echo `date` "Determining if I should run a ChannelSelector"
    if java gov.fnal.ppd.dd.util.HasChannelSelector; then
	if ps -aef | grep MakeChannelSelector | grep -v grep; then
	    echo "Already running the ChannelSelector."
	else
	    echo "Starting the ChannelSelector";
	    ./runSelector.sh SKIP
	    sleep 20;
	fi
    fi

    echo "Running the display software ..."

    # Remove the old Channel serialzed files that might still exist
    rm -f *.ser 

    # An exit code of -1 (255 here) is going to mean that there was a problem from which we should try to recover.

    while {
        # Do we need to run two instances of the display?  Then fix the position of the firefox windows
	./fixFirefoxConfig.sh

	java -Xmx1024m gov.fnal.ppd.dd.display.client.DisplayAsConnectionToFireFox 
	test $? -eq 255
    }
    do
	echo ""
	echo ""
	echo ""
	echo `date` " Display program exited with failure ..."
	sleep 15
	echo `date` " Trying again now."
	echo ""
	echo ""
	echo ""
    done

    echo ""
    echo ""
    echo ""
    echo "Display program was killed.  Bye."

} >> $log 2>&1 &
