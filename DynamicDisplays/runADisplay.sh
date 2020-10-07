#!/bin/bash

# Launch the Dynamic Displays program(s) that this node should be running.  The options are
#  -- The messaging server
#  -- The Channel Selector GUI
#  -- The display

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

# --------------------------------------------------------------------------------
# Idiot Check - Don't run if we are almost out of disk space

ONEGIG=1048576
# Assuming that df returns kilobytes remaining in column 4
let GB=`df | grep home | awk '{ print $4 }'`/$ONEGIG
minimum=3
if [ $GB -lt $minimum ]; then
    echo Insufficient disk space, $GB GB, to run the Dynamic Displays software.  Log files for this application and for the system need at least $minimum GB.
    echo Here is the df command.
    df 
    echo
    echo This situation is unexpected.  Exiting.
    text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"40000\">\n        Insufficient Disk Space, " $GB "GB,\n                        to run the\n            Dynamic Display Software\n</span>"
    zenity --error --width=900 --title="Dynamic Displays Software Fatal Error B" --text=$text
    exit;
fi >> $log 2>&1

# Setup executables location
workingDirectory=$ddHome/roc-dynamicdisplays/DynamicDisplays

# Verify that this script is not running now
if ps -aef | grep $workingDirectory/$0 | grep -v grep ; then
    echo `date` It looks like this script is already running 
    exit 1;
fi >> $log 2>&1

cd $workingDirectory

echo `date` Working directory is `pwd` >> $log 2>&1

# Removed the check for the new version - this is in the script that calls us here.

cd $workingDirectory

# Remove the json file that seems to be responsible for configuring Firefox.
# In particular, this holds the last location of the Firefox windows.
# But this does not seem to have the desired effect (3/2018) - more work needed.
# ls -l $HOME/.mozilla/firefox/*Dynamic*/*.json >> $log 2>&1
# echo Removing xulstore.json files >> $log 2>&1
# rm -fv $HOME/.mozilla/firefox/*Dynamic*/xulstore.json >> $log 2>&1

MyName=`uname -n`

. setupJars.sh

screenNum=0
if [ "$1 X" != " X" ]; then
    screenNum=$1;
fi
{
    echo `date` "Obtaining the messaging server, and determining if it is this node ... "
    # Get the messaging server for me
    messagingServer=`java -Xmx512m gov.fnal.ppd.dd.GetMessagingServer | grep "MessagingServer=" | awk '{ print $2 }'`

    # Am I a messaging server?
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
    if java gov.fnal.ppd.dd.util.specific.HasChannelSelector; then
	if ps -aef | grep MakeChannelSelector | grep -v grep; then
	    echo "Already running the ChannelSelector."
	else
	    echo "Starting the ChannelSelector";
	    ./runSelector.sh SKIP
	    # Give it a head start before starting the display software
	    sleep 2;
	fi
    fi

    echo `date` "Determining if this node should run a display ..."

    if java gov.fnal.ppd.dd.util.specific.IsDisplayNode; then
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
