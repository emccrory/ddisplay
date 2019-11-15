#!/bin/bash

# Should I run a full-on Dynamic Display, or should I run the super-simplified Firefox-only display?

. setupJars.sh

# Set up log file 
ddHome=$HOME/src
node=`uname -n`
if [ $node = "ad130482.fnal.gov" ]; then
    ddHome=/home/mccrory/git-ddisplay
fi

log=$ddHome/log/displayStartup.log

if [ -e $log ] ; then
    # Rename the existing log file with time stamp of the first access (creation time)
    # This command pipe Assumes A LOT!  So it will probably be brittle
   suffix=`stat $log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`

   mv $log $ddHome/log/displayStartup_$suffix.log
   gzip    $ddHome/log/displayStartup_$suffix.log &
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

{
    cd $workingDirectory
    # Prepare to run the Java applications
    . setupJars.sh
    
    # --------------------------------------------------------------------------------
    # Do not begin until we can ping the database server
    # --------------------------------------------------------------------------------
    
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

    # --------------------------------------------------------------------------------
    # Now decide if we run a full version or the simpler, less capable version.
    # --------------------------------------------------------------------------------

    temp=temp$$
    java gov.fnal.ppd.dd.db.GetDefaultContentForDisplay > $temp
    
    displayType=`grep DISPLAYTYPE $temp | awk '{ print $2 }'`
    
    if [ $displayType = "Regular" ]; then
	./runADisplay.sh &
    else
	./startFirefoxOnly.sh &
    fi
    
    rm $temp
} >> $log 2>&1
