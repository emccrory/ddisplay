#!/bin/bash

# The script for doing everything that needs to be done to start an instance of a Dynamic Displays display.
# Look out, though.  This has gotten quite complicated over the years, even thought the part that actually
# runs the Java program for the display has been factored out into another script (at the end).

. setupJars.sh

# Set up log file 
ddHome=$HOME/src
node=`uname -n`
if [ $node = "ad130482.fnal.gov" ]; then
    # This is the desktop Linux PC on McCrory's desk.
    ddHome=/home/mccrory/git-ddisplay
fi

# --------------------------------------------------------------------------------
# Idiot Checks - Don't run if:
#   -- the most recent log file is very new (indicating we might be in an infinite loop)
#   -- we are almost out of disk space
#

# Set up the log file for the startup process and check that it is not from a moment ago
log=$ddHome/log/displayStartup.log

# ----- Check if the old log file is relatively new
minutes=3
if [ -e $log ] ; then
    # Check the date on the old log file and stop if it is "too new"
    if test `find "$log" -type f -mmin -$minutes` ; then 
	ls -l $log
	text="Log file $log was modified less than $minutes mintues ago.\n\nStopping this script since this indicates that we might be in an infinite loop."
	zenity --error --width=900 --title="Dynamic Displays Software Fatal Error C" --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"20000\">$text</span>"
	exit;
    fi
    # Rename the existing log file with time stamp of the first access (creation time)
    # This command pipe Assumes A LOT!  So it will probably be brittle
   suffix=`stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`

   mv $log $ddHome/log/displayStartup_$suffix.log
   gzip    $ddHome/log/displayStartup_$suffix.log &
fi

touch $log

# ----- Check if there are "several new" instances of roc-dynamicdisplays-old??? in the src folder

cd $ddHome
t=temp_$$
minutes=$(( $minutes * 2 ))

for i in `ls -td roc-dynamicdisplays-old??? `; do
    # Which are newer than 10 minutes?
    find $i -mmin +$minutes
done | wc -l > $t 2>/dev/null
if [ `cat $t` -gt 2 ]; then
    # We saw 3 or more of the roc-dynamicdisplays-old* folders that were new
    text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"40000\">\nToo many instances of recent\nroc-dynamicdisplays-old folders - Stopping now.</span>";
    zenity --error --width=900 --title="Dynamic Displays Software Fatal Error D" --text=$text
    exit
fi

# ----- Check disk usage
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
    zenity --error --width=900 --title="Dynamic Displays Software Fatal Error A" --text=$text
    exit;
fi >> $log 2>&1
# --------------------------------------------------------------------------------

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
fi >> $log 2>&1

# -----------------------------------------------------------------------------------------------------
# This is the heart of the script.  Put it into a block of code so we can pipe it all to the log file.

{
    cd $workingDirectory
    # Prepare to run the Java applications
    . setupJars.sh
    
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

    # --------------------------------------------------------------------------------
    # Do not begin until we can ping the database server
    # --------------------------------------------------------------------------------
    
    dbs=`echo $databaseServer | sed 's/:/ /g' | awk '{ print $1 }'`
    sleepTime=5

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

    # --------------------------------------------------------------------------------
    # Decide if we run a full version or the simpler, less capable version.
    # --------------------------------------------------------------------------------

    temp=temp$$
    java gov.fnal.ppd.dd.db.GetDefaultContentForDisplay > $temp
    
    displayType=`grep DISPLAYTYPE $temp | awk '{ print $2 }'`
    
    if [ $displayType = "Regular" ]; then
	./runADisplay.sh &
    else
	./startFirefoxOnly.sh &
    fi
    
    # Since the start commands are pushed into the background, we are done now.

    rm $temp
} >> $log 2>&1
