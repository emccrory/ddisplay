#!/bin/bash

# TODO - If we have too much trouble starting the Java/Selenium/Firefox connection,
# it should revert to running the script startFirefoxOnly.sh

# If we are being called by this script (see the update block, below), we should wait a second for that script to finish
if [ ! "$1 X" = " X" ]; then
    sleep $1
fi

d=`date +%F`

# To be compatible with non-bash shells (e.g., on a Mac), we are using $HOME instead of the more succinct ~

# Set up log file 
ddHome=$HOME/src
node=`uname -n`
if [ $node = "ad130482.fnal.gov" ]; then
    ddHome=/home/mccrory/git-ddisplay
fi

log=$ddHome/log/display.log

if [ -e $log ] ; then
    # Rename the existing log file with time stamp of the first access (creation time)
    # This command pipe Assumes A LOT!  So it will probably be brittle
   suffix=`stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`

   mv $log $ddHome/log/display_$suffix.log
   gzip    $ddHome/log/display_$suffix.log &
fi

touch $log

# Setup executables location
workingDirectory=$ddHome/roc-dynamicdisplays/DynamicDisplays

# Verify that this script is not running now
if ps -aef | grep $workingDirectory/$0 | grep -v grep ; then
    echo `date` It looks like this script is already running 
    exit 1;
fi >> $log 2>&1

cd $workingDirectory

echo `date` Working directory is `pwd` >> $log 2>&1

# Check the version of the code
if ( ./runVersionInformation.sh Y  ); then
    echo "There is a new version, which we have retrieved.  Restarting this script."
    cd $workingDirectory
    exec $0 2
    exit 0
fi >> $log 2>&1

# Test for and remove the cache file from disk
if [ -e $HOME/.mozilla/firefox/*.default/places.sqlite ]; then
    cd $HOME/.mozilla/firefox/*.default
    ls -l  places.sqlite
    echo Removing disk-based history/cache
    rm -fv places.sqlite 
fi >> $log 2>&1

# We need something to run on the X display, otherwise the present version of FireFox, with the
# kiosk mode enabled, won't let us get to the desktop
cd $ddHome/log

# Kill the exiting bash scripts that are in the src/log directory
tempPIDs=temp$$
pwdx `ps -aef | grep ddisplay | grep "00:00:00 bash" | grep -v grep | grep -v cron | grep -v $0 | awk '{ print $2 }'` 2>/dev/null | grep /log | awk '{ print $1 }' | sed 's/://g' > $tempPIDs
if [ -s $tempPIDs ]; then
    kill -9 `cat $tempPIDs`
fi
rm $tempPIDs

if [ -e /usr/bin/xterm ]; then
    # SLF 6.x
    /usr/bin/xterm -geometry 200x30 -fa 'Monospace' -fs 12 &
elif [ -e /usr/bin/gnome-terminal ]; then
    # SL 7.x
    /usr/bin/gnome-terminal --geometry 200x30 --zoom=1.5 &
elif [ -e /opt/X11/bin/xterm ]; then
    # Mac OS
    # First, wait for the X Server to initialize; starting the xterm usually seems to do it.
    /opt/X11/bin/xterm -e 'echo "Assuring that X11 is running ..."; sleep 5'
    # Now start up a permanent X terminal so we can use it to look at log files and stuff
    /opt/X11/bin/xterm -geometry 200x30 &
fi >> $log 2>&1

# Remove the json file that seems to be responsible for configuring Firefox.
# In particular, this holds the last location of the Firefox windows.
# But this does not seem to have the desired effect (3/2018) - more work needed.
# ls -l $HOME/.mozilla/firefox/*Dynamic*/*.json >> $log 2>&1
# echo Removing xulstore.json files >> $log 2>&1
# rm -fv $HOME/.mozilla/firefox/*Dynamic*/xulstore.json >> $log 2>&1

{
    cd $workingDirectory
    # Prepare to run the Java applications
    . setupJars.sh
    
    # Do not begin until we can ping the database server
    
    dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
    sleepTime=5

    # In Linux-land, make sure the screen saver and screen blankers are off.
    # keep this here for reference, but do not do it uniformly (e.g., not my desktop)
    if [ "Skip" = "This" ]; then
	XSET=`command -v xset`
	
	if [ "$XSET X" = " X" ]; then
	    echo Cannot assure that the screen saver / screen blanker is off; 
	else 
	    echo Turning off screen saver and screen blanker using $XSET
	    export DISPLAY=:0.0
	    $XSET s off
	    $XSET s noblank
	    $XSET -dpms
	fi
    fi

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

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
    echo `date` "Obtaining the messaging server, and determining if it is this node ... "
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
	    # Give it time to start before trying to start the display software (it does not need much)
	    sleep 2;
	fi
    else
	echo `date` "The messaging server is " $messagingServer ", which is not this node"
    fi
    
    echo `date` "Determining if I should run a ChannelSelector"
    if java gov.fnal.ppd.dd.util.HasChannelSelector; then
	if ps -aef | grep MakeChannelSelector | grep -v grep; then
	    echo "Already running the ChannelSelector."
	else
	    echo "Starting the ChannelSelector";
	    ./runSelector.sh SKIP
	    # Give it a head start before starting the display software
	    sleep 2;y
	fi
    fi

    echo `date` "Determining if this node should run a display ..."

    if java gov.fnal.ppd.dd.util.IsDisplayNode; then
	if ps -aef | grep DisplayAs | grep -v grep; then
	    echo "Already running the display software."
	    exit;
	fi
        # Remove the old Channel serialzed files that might still exist
	rm -f *.ser 

	# Make sure all of the selenium drivers are executable
	chmod +x lib/selenium/*driver
	
        # An exit code of -1 (255 here) is going to mean that there was a problem from which we should try to recover.
	while {
 	    # Diagnostic to record if there is someone who has port 49999 open (to the messaging server)
	    lsof -i :49999

	    # Kill previously started versions of geckodriver 
            # (TODO - this should be handled automatically, but this seems to be an active Selenium issue)
	    for i in `ps -aef | grep "lib/selenium/geckodriver" | grep -v grep | awk '{ print $2 }'`; do
		echo "Killing a previous version of geckodriver"
		kill $i;
	    done;
	    
            # Do we need to run two instances of the display?  Then fix the position of the firefox windows
	    # ./fixFirefoxConfig.sh -- not working on all display nodes.
	    
	    java -Xmx1024m gov.fnal.ppd.dd.display.client.selenium.DisplayAsConnectionThroughSelenium 
	    
            # This command establishes the exit code of the while-loop test.  Looking for exit code of -1
	    test $? -eq 255
	}
	do
	    echo ""
	    echo ""
	    echo ""
	    echo `date` " Display program exited with an understood failure ..."
	    echo Restarting the display on `hostname` | /usr/bin/mail -s "Display software has restarted" mccrory@fnal.gov
	    sleep 5
  	    # Maybe there is a new version of the software here.  
	    # This "cd" should put us in the right place (unless the new version contains a new version of this script.)
	    cd $workingDirectory
	    echo ""
	    echo `date` " Exec'ing " $0 ".  Working directory is" `pwd`
	    echo ""
	    echo ""
	    echo ""
	    exec $0
	    # OK, the new version of this script is now running.
	done
	
	echo ""
	echo ""
	echo ""
	echo `date` Either the display program was killed, or it got an unrecognized error.  Bye.
    fi
} >> $log 2>&1 &