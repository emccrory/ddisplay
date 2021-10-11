#!/bin/bash

# Start the messaging server.  
# There are several things that need to happen to be sure that the environment is right.

# If we are being called by this script (see the update block, below), we should wait a second for that script to finish
if [ ! "$1 X" = " X" ]; then
    sleep "$1"
fi

ddHome="$HOME/src"
node=$(uname -n)
adminNode="ad130482.fnal.gov"
adminWorkspace="/home/mccrory/git-ddisplay"
if [ "$node" = "$adminNode" ]; then
    ddHome=$adminWorkspace
fi

cd "$ddHome/roc-dynamicdisplays/DynamicDisplays" || exit
. setupEnvironment.sh

cd ../../log || exit

for logFile in messagingServerOther messagingServerWarnings messagingServer; do
    if [ -e $logFile.log ]; then
	# Rename the existing log files with time stamp of the first access (creation time)
	suffix=$(stat $logFile.log | grep "Access: 2" | cut -b 9-27 | sed 's/ /_/g' | sed 's/:/./g')
	# This command pipe ASSUMES A LOT!  So expect it to be brittle
	
	mv "${logFile}.log" "${logFile}_$suffix.log"
    fi
done

# This compression might take some time.  Wait for it so the log file doesn't get overwritten
gzip messagingServerOther_*.log messagingServerWarnings_*.log messagingServer_*.log 
mv ./*.gz oldLogs

cd ../roc-dynamicdisplays/DynamicDisplays || exit
workingDirectory=$(pwd)

logFile=messagingServerWarnings.log
log="../../log/$logFile"

touch "$log"
{
    # Note: Most of the messages from the server are funneled through a java.lang.util.Logger class object.  These
    # go to messagingServer.log.  Everything else goes to messagingServerWarnings.log.  

    # The messages in this file are:
    #  -- "echo" statements here, 
    #  -- many of the warning messages from MessageConveyor,
    #  -- messages from JavaVersion
    
    # Verify that this script is not running now
    if pgrep -af "$workingDirectory/$0" ; then
	echo "$(date)" It looks like this script is already running 
	exit 1;
    fi

    echo "$(date) Working directory is $workingDirectory"

    # Check the version of the code
    if ( ./runVersionInformation.sh Y  ); then
	echo "There is a new version, which we have retrieved.  Restarting this script."
	cd "$workingDirectory" || exit
	exec "$0" 2
	exit 0
    fi 

    # Do not need to wait on the DB server, so forge ahead!

    while {
 	# Diagnostic to record if there is someone who already has our messaging port open
	port=$(./property.sh messagingPort)
	/usr/sbin/lsof -i :"$port"
	
	# ------------------------------------------------------------------------------------------
	# -----  Here is the messaging server daemon startup ---------------------------------------

	java -Xmx1024m gov.fnal.ppd.dd.chat.MessagingServerDaemon

	# ------------------------------------------------------------------------------------------

        # This command establishes the exit code of the while-loop test.  Looking for exit code of -1
	test "$?" -eq 255
    }
    do
	echo ""
	echo ""
	echo ""
	echo "$(date) Messaging server daemon exited with an understood failure ..."
	mailapp=/usr/bin/mail
	if [ -x $mailapp ]; then
	    echo Restarting the messaging server on "$(hostname)" | $mailapp -s "Messaging server has restarted" "$adminEmail"
	else
	    echo "Mail application, $mailapp, is not installed on this node"
	fi
	sleep 5

  	#
	# The assumption here is that THIS SCRIPT will perform the update, NOT the Java app.
	#
	
	# Check the version of the code
	if ( ./runVersionInformation.sh Y  ); then
	    echo "There is a new version, which we have retrieved.  Restarting this display application."
	fi 

	# Note that this process is running the "old" version of the script.  
	# The new suite version may have a new version of this script.
	# This "cd" should put us in the right place 
	cd "$workingDirectory" || exit
	echo ""
	echo "$(date) Exec'ing  $0.  Working directory is $(pwd)"
	echo ""
	echo ""
	echo ""
	exec "$0" || exit
	# OK, the new version of this script should be running.
    done
    
    echo ""
    echo ""
    echo ""
    echo "$(date) Either the messaging server daemon was killed, or it got an unrecognized error.  Bye."
} >> $log 2>&1

