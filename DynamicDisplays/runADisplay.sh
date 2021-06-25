#!/bin/bash

# Launch the Dynamic Displays program(s) that this node should be running.  The options are
#  -- The messaging server
#  -- The Channel Selector GUI
#  -- The display

# TODO - If we have too much trouble starting the Java/Selenium/Firefox connection,
# it should revert to running the script startFirefoxOnly.sh

# If we are being called by this script (see the update block, below), we should wait a second for that 
# script to finish and to let the log files age out properly.
if [ ! "$1 X" = " X" ]; then
    sleep "$1"
fi

# To be compatible with non-bash shells (e.g., on a Mac), we are using $HOME instead of the more succinct ~

# Set up log file 
ddHome=$HOME/src
node=$(uname -n)
adminNode="ad130482.fnal.gov"
adminWorkspace="/home/mccrory/git-ddisplay"
if [ "$node" = "$adminNode" ]; then
    ddHome=$adminWorkspace
fi

log="$ddHome/log/display.log"

# Are there any old zip files in the log folder?  If so, move them to the archive folder
if ( ls $ddHome/log/*.gz >/dev/null 2>&1 ) then
    mv $ddHome/log/*.gz "$ddHome/log/oldLogs"
fi

tempLog=tempLog_$$
{
    if [ -e "$log" ] ; then
	# Rename the existing log file with time stamp of the first access (creation time)
	# This command pipe Assumes A LOT!  IT WILL BE BRITTLE
	suffix=$(stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g')
	
	mv "$log" "$ddHome/log/display_$suffix.log"
	gzip      "$ddHome/log/display_$suffix.log" 
    fi
} > $tempLog 2>&1

mv "$tempLog" "$log"

# Setup executables location
workingDirectory=$ddHome/roc-dynamicdisplays/DynamicDisplays

# Verify that this script is not running now
pgrep -a "$workingDirectory/$0" && {  echo "$(date)" "It looks like $0 is already running"; exit 1; } >> $log 2>&1

cd "$workingDirectory" || exit
. setupEnvironment.sh

echo "$(date)" Working directory is "$(pwd)" >> $log 2>&1

# Removed the check for the new version - this is in the script that calls us here.

cd "$workingDirectory" || exit

# --------------------------------------------------------------------------------
# Idiot Checks - Don't run if we are almost out of disk space

./checkDiskSpace.sh "$0" || { echo "$0: Insufficient disk space" ; exit 1; } >> $log 2>&1

# Remove the json file that seems to be responsible for configuring Firefox.
# In particular, this holds the last location of the Firefox windows.
# But this does not seem to have the desired effect (3/2018) - more work needed.
# ls -l $HOME/.mozilla/firefox/*Dynamic*/*.json >> $log 2>&1
# echo Removing xulstore.json files >> $log 2>&1
# rm -fv $HOME/.mozilla/firefox/*Dynamic*/xulstore.json >> $log 2>&1

MyName=$(uname -n)

{
    # This used top check to see if this node is supposed to run the messaging server.  This is not relevant anymore.
    # The node designated to run the messaging server will run the startup script directly after a reboot.

    echo "$(date) Determining if I should run a ChannelSelector"
    if java gov.fnal.ppd.dd.util.specific.HasChannelSelector; then
	if pgrep -a MakeChannelSelector ; then
	    echo "Already running the ChannelSelector."
	else
	    echo "Starting the ChannelSelector";
	    ./runSelector.sh SKIP
	    # Give it a head start before starting the display software
	    sleep 2;
	fi
    fi

    echo "$(date)" "Determining if this node should run a display ..."

    if java gov.fnal.ppd.dd.util.specific.IsDisplayNode; then
	if pgrep -a DisplayAs; then
	    echo "Already running the display software."
	    exit;
	fi
        # Remove the old Channel serialzed files that might still exist
	rm -f ./*.ser 

	# Make sure all of the selenium drivers are executable
	chmod +x lib/selenium/*driver
	
        # An exit code of -1 (255 here) is going to mean that there was a problem from which we should try to recover.
	while {
 	    # Diagnostic to record if there is someone who has our port to the messaging server open
	    port=$(./property.sh messagingPort)
	    lsof -i :"$port"

	    # Kill previously started versions of geckodriver 
            # (TODO - this should be handled automatically, but this seems to be an active Selenium issue)
	    if pkill "lib/selenium/geckodriver"; then
		echo "Killed previous running versions of geckodriver"
	    fi
	    
            # Do we need to run two instances of the display?  Then fix the position of the firefox windows
	    # ./fixFirefoxConfig.sh -- not working on all display nodes.
	    
	    # ------------------------------------------------------------------------------------------
	    # ----- Here is the startup of the display software  ---------------------------------------
	    # ------------------------------------------------------------------------------------------

	    java -Xmx1024m gov.fnal.ppd.dd.display.client.selenium.DisplayAsConnectionThroughSelenium 

	    # ------------------------------------------------------------------------------------------

            # This command establishes the exit code of the while-loop test.  Looking for exit code of -1
	    test $? -eq 255
	}
	do
	    echo ""
	    echo ""
	    echo ""
	    echo "$(date)" " Display program exited with an understood failure ..."
	    mailapp=/usr/bin/mail
	    if [ -x $mailapp ] then
		echo Restarting the display on "$(hostname)" | $mailapp -s "Display software has restarted" "$adminEmail"
	    else
		echo "Mail application, $mailapp, is not installed on this node"
	    fi
	    sleep 5
	    #
	    # The assumption here is that this script will perform the update, NOT the Java app.
	    #

	    # Check the version of the code
	    if ( ./runVersionInformation.sh Y  ); then
		echo "There is a new version, which we have retrieved.  Restarting this display application."
	    fi 
	    
	    # This "cd" should put us in the right place (unless the new version contains a new version of this script.)
	    cd "$workingDirectory" || exit
	    echo ""
	    echo "$(date) Exec'ing $0.  Working directory is $(pwd)"
	    echo ""
	    echo ""
	    echo ""
	    exec "$0" 3 || exit
	    # OK, the new version of this script is now running.
	done
	
	echo ""
	echo ""
	echo ""
	echo "$(date)" Either the display program was killed, or it got an unrecognized error.  Bye.
    fi
} >> $log 2>&1 &
