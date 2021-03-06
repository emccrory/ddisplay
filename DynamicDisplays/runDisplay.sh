#!/bin/bash

# This is the script for doing everything that needs to be done to start an instance 
# of a Dynamic Displays display. Look out, though.  This has gotten quite complicated 
# over the years, even thought the part that actually runs the Java program for the 
# display has been factored out into another script (at the end).

initialTemp=displayPreliminary_$$

{
    if [ "$1 X" != " X" ]; then
	# We are being called by this script.  In order to have the log file date check succeed, wait.
	echo "Sleeping for a bit prior to getting started."
	sleep $(($1*60+30))
	export haveBeenCalled=1
    fi

    # Where are we?
    ddHome=$HOME/src
    node=$(uname -n)
    
    adminNode="ad130482.fnal.gov"
    adminWorkspace="/home/mccrory/git-ddisplay"
    if [ "$node" = "$adminNode" ] ; then
	# This is the desktop Linux PC on McCrory's desk.
	ddHome=$adminWorkspace
    fi
    
    # Set up the log file for the startup process and check that it is not from a moment ago
    log="$ddHome/log/displayStartup.log"
    
    # ---------------------------------------------------------------------------------------
    # Sanity Checks - Don't run if:
    #   -- the most recent log file is very new (indicating we might be in an infinite loop)
    #   -- we are almost out of disk space
    #
    
    echo Checking if the old log file is relatively new so that we are not in a tight restart loop
    minutes=3
    if [ -e "$log" ] ; then
	# Check the date on the old log file and stop if it is "too new"
	if test "$(find $log -type f -mmin -$minutes)" ; then
	    text="Log file $log was modified less than $minutes mintues ago.\n\nThis is an error condition because it might mean\nthat we are an infinite loop."
	    echo "$text"
	    ls -l $log
	    zenity --error --width=900 --title="Dynamic Displays Software Fatal Error C" --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"20000\">$text</span>"
	    exit;
	fi
	echo Renaming the existing log file with time stamp of the first access, creation time.
	# This command pipe ASSUMES A LOT!  So it will probably be brittle
	suffix=$(stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g')
	
	mv "$log" "$ddHome/log/displayStartup_$suffix.log"
	gzip      "$ddHome/log/displayStartup_$suffix.log" 
    fi 

} > "$initialTemp" 2>&1

# Preliminaries completed - go to the final log file.
mv "$initialTemp" "$log"

{
    # ----- Check if there are "several new" instances of roc-dynamicdisplays-old??? in the src folder

    cd $ddHome || exit

    minutesFolder=$(( minutes * 2 ))

    t=temp_$$
    for i in roc-dynamicdisplays-old???; do
	# Which are newer than the specified number of minutes?
	[[ -e "$i" ]] || break;
	find "$i" -mmin -$minutesFolder
    done | wc -l > $t 2>/dev/null
    if [ "$(cat $t)" -gt 2 ]; then
	# We saw 3 or more of the roc-dynamicdisplays-old* folders that were new
	text="\nToo many 'new' instances of recent\nroc-dynamicdisplays-old folders - Stopping now.";
	echo "$text"
	zenity --error --width=900 --title="Dynamic Displays Software Fatal Error D" --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"40000\">$text</span>"
	exit
    fi
    
    # Setup executables location
    workingDirectory=$ddHome/roc-dynamicdisplays/DynamicDisplays
    
    # Verify that this script is not running now
    pgrep -a "$workingDirectory/$0" && { date; echo "It looks like $0 is already running"; exit 1; }

    cd $workingDirectory || exit
    . setupEnvironment.sh

    # ----- Check disk usage
    ./checkDiskSpace.sh "$0" || { echo "$0: Insufficient disk space" ; exit 1; }

    date
    echo Working directory is "$(pwd)" 

    # Check the version of the code
    if [ "$haveBeenCalled X" = " X" ]; then
	if ( ./runVersionInformation.sh Y  ); then
	    min=4
	    echo "There is a new version of the Dynamic Displays software, which we have retrieved.  Restarting this script in $min minutes."
	    cd $workingDirectory || exit
	    exec "$0" "$min"
	    exit 0
	fi 
    else
	echo "Skipping version check since we were just called to do this a few minutes ago."
    fi

    # Test for and remove the cache file from disk
    if [ -e "$HOME/.mozilla/firefox/*.default/places.sqlite" ]; then
	cd "$HOME/.mozilla/firefox/*.default" || exit
	ls -l  places.sqlite
	echo Removing disk-based history/cache
	rm -fv places.sqlite 
    fi 

    # We need something to run on the X display, otherwise the present version of FireFox, with the
    # kiosk mode enabled, won't let us get to the desktop
    cd $ddHome/log || exit
    
    # Kill the exiting bash scripts that are in the src/log directory
    tempPIDs=temp$$
    tempOut=$(pgrep -a ddisplay | grep "00:00:00 bash" | grep -v cron | grep -v "$0" | awk '{ print $2 }')
    pwdx "$tempOut" 2>/dev/null | grep /log | awk '{ print $1 }' | sed 's/://g' > "$tempPIDs"
    if [ -s "$tempPIDs" ]; then
	kill -9 "$(cat "$tempPIDs")"
    fi
    rm "$tempPIDs"
    
    # Find the best terminal to start (depending on the OS)  Note, we are SOOL for Windows
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
    fi
    
# -----------------------------------------------------------------------------------------------------
# This is the heart of the script.  Put it into a block of code so we can pipe it all to the log file.

    cd $workingDirectory || exit
    
    # In Linux-land, make sure the screen saver and screen blankers are off.
    # keep this here for reference, but do not do it uniformly (e.g., not my desktop)
    skip="Skip this"
    block="block of code"
    if [ "$skip" = "$block" ]; then
	XSET=$(command -v xset)
	
	if [ "$XSET X" = " X" ]; then
	    echo Cannot assure that the screen saver / screen blanker is off; 
	else 
	    echo Turning off screen saver and screen blanker using "$XSET"
	    export DISPLAY=:0.0
	    $XSET s off
	    $XSET s noblank
	    $XSET -dpms
	fi
    fi

    # --------------------------------------------------------------------------------
    # Do not begin until we can ping the database server
    # --------------------------------------------------------------------------------
    
    dbs=$(echo "$databaseServer" | sed 's/:/ /g' | awk '{ print $1 }')
    sleepTime=5

    while :
    do
    # Forever loop. We assume that the DB server will appear eventually
	if ping -c 1 "$dbs"
	then
	    break
	else
	    date
	    echo Waiting "$sleepTime" seconds for the DB server "$dbs" to be visible 
	    sleep $sleepTime;
	fi
	sleepTime=$((sleepTime+2));
	if [ "$sleepTime" -gt 150 ]; then
	    sleepTime=150; # It looks like we are going to be here a while.  Recheck every 2.5 minutes.
	fi
    done

    # --------------------------------------------------------------------------------
    # Decide if we run a full version or the simpler, less capable version.
    # --------------------------------------------------------------------------------

    temp=temp$$
    java gov.fnal.ppd.dd.db.GetDefaultContentForDisplay > $temp
    
    displayType=$(grep DISPLAYTYPE "$temp" | awk '{ print $2 }')
    reg="Regular/Selenium"
    ff="FirefoxOnly"
    if [ "$displayType" = "$reg" ]; then
	echo "$0 - Running the regular, Selenium-based browser"
	./runADisplay.sh &
    elif [  "$displayType" = "$ff" ]; then
	echo "$0 - Running the simplified, Firefox-only browser.  This version is less capable than the regular version."
	./startFirefoxOnly.sh &
    else
	text1="\nThe type of browser connection, '$displayType', is undefined.";
	text2="\nExpected '$reg' '$ff'";
	text3="\n\nScript $0 - Take a picture of this and send it to an expert.";
	echo "$text1"
	echo "$text2"
	echo "$text3"
	zenity --error --width=900 --title="Dynamic Displays Software Fatal Error E" --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"40000\">$text1</span><span font-family=\"sans\" font-weight=\"400\" font-size=\"20000\">$text2</span><span font-family=\"sans\" font-weight=\"400\" font-size=\"10000\">$text3</span>"
    fi
    
    # Since the start commands are pushed into the background, we are done now.

    rm "$temp"
} >> $log 2>&1
