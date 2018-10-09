#!/bin/bash

# If we are being called by this script (see the update block, below), we should wait a second for that script to finish
if [ ! "$1 X" = " X" ]; then
    sleep $1
fi

d=`date +%F`

# To be compatible with non-bash shells (e.g., on a Mac), we are using $HOME instead of the more succinct ~

# Set up log file 
# log=$HOME/src/log/display_${d}_$$.log
log=$HOME/src/log/display.log

if [ -e $log ] ; then
   mv $log $HOME/src/log/display_${d}_$$.log
   gzip $HOME/src/log/display_${d}_$$.log
fi

touch $log

# Setup executables location
workingDirectory=$HOME/src/roc-dynamicdisplays/DynamicDisplays

# Verify that this script is not running now
if ps -aef | grep $workingDirectory/$0 | grep -v grep ; then
    echo `date` It looks like this script is already running 
    exit 1;
if >> $log 2>&1

cd $workingDirectory

echo `date` `pwd` >> $log 2>&1

# Check the version of the code
if ( ./runVersionInformation.sh Y  ); then
    cd $workingDirectory
    ./runDisplay.sh 2 &
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
cd $HOME/src/log

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
# WrapperType=NORMAL
WrapperType=FRAMENOTICKER # 3

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

    echo `date` "Determining if this node should run a display ..."

    if java gov.fnal.ppd.dd.util.IsDisplayNode; then
	if ps -aef | grep DisplayAs | grep -v grep; then
	    echo "Already running the display software."
	    exit;
	fi
        # Remove the old Channel serialzed files that might still exist
	rm -f *.ser 
	
        # An exit code of -1 (255 here) is going to mean that there was a problem from which we should try to recover.
	while {
 	    # Diagnostic to record if there is someone who has port 49999 open (to the messaging server)
	    lsof -i :49999

	    # Kill previously started versions of geckodriver 
            # (TODO - this should be handled in the Java, but this seems to be an active Selenium issue)
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
	    sleep 15
  	    # Maybe there is a new version of the software here.  
	    # This "cd" should put us in the right place (unless the new version contains a new version of this script.)
	    cd $workingDirectory
	    echo "     ..."
	    echo `date` " Trying again now."
	    echo ""
	    echo ""
	    echo ""
	done
	
	echo ""
	echo ""
	echo ""
	echo `date` Either the display program was killed, or it got an unrecognized error.  Bye.
    fi
} >> $log 2>&1 &
