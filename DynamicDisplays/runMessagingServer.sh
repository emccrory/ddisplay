#!/bin/bash

# Start the messaging server.  There are several things that need to happen to be sure that the
# environment is right.

# If we are being called by this script (see the update block, below), we should wait a second for that script to finish
if [ ! "$1 X" = " X" ]; then
    sleep $1
fi

ddHome=$HOME/src
node=`uname -n`
if [ $node = "ad130482.fnal.gov" ]; then
    ddHome=/home/mccrory/git-ddisplay
fi

cd $ddHome/roc-dynamicdisplays/DynamicDisplays

. setupJars.sh

cd ../../log

logFile=messagingServer.log

if [ -e $logFile ]; then
    # Rename the existing messagingServer.log with time stamp of the first access (creation time)
    suffix=`stat $logFile | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g'`
    # This command pipe ASSUMES A LOT!  So expect it to be brittle

    mv $logFile messagingServer_$suffix.log
fi

# This compression might take some time, so push it into the background
gzip messagingServer_*.log &

cd ../roc-dynamicdisplays/DynamicDisplays
workingDirectory=`pwd`

log=../../log/$logFile

touch $log

# Verify that this script is not running now
if ps -aef | grep $workingDirectory/$0 | grep -v grep ; then
    echo `date` It looks like this script is already running 
    exit 1;
fi >> $log 2>&1

echo `date` Working directory is $workingDirectory >> $log 2>&1

# Check the version of the code
if ( ./runVersionInformation.sh Y  ); then
    echo "There is a new version, which we have retrieved.  Restarting this script."
    cd $workingDirectory
    exec ./runMessagingServer.sh 2
    exit 0
fi >> $log 2>&1

{
    # Do not need to wait on the DB server, so forge ahead!

    while {
 	# Diagnostic to record if there is someone who already has port 49999 open 
	lsof -i :49999
	
	#################### And now, finally, here is the messaging server daemon startup

	java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerDaemon

	####################

        # This command establishes the exit code of the while-loop test.  Looking for exit code of -1
	test $? -eq 255
    }
    do
	echo ""
	echo ""
	echo ""
	echo `date` " Messaging server daemon exited with an understood failure ..."
	echo Restarting the messaging server on `hostname` | /usr/bin/mail -s "Messaging server has restarted" mccrory@fnal.gov
	sleep 15
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
    echo `date` Either the messaging server daemon was killed, or it got an unrecognized error.  Bye.
} >> $log 2>&1

