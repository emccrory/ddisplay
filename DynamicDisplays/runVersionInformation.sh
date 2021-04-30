#!/bin/bash

#
# Script to learn if there is a newer version of the software available from the server.
#

. setupJars.sh

# ----------------------------------------------------------------------
# IDIOT CHECK: Is there enough disk space to proceed?

./checkDiskSpace.sh $0 || { echo "$0: Insufficient disk space" ; exit 1; } 

# End of IDIOT Check ---------------------------------------------------

# for the display software, use the command line argument, "Y" to do the update, no questions asked.
yes=0;
if [ "$1 X" = "Y X" ]; then
    yes=1;
fi

d=$(date +%F)

#
# Evaluate if there is a newer version of the software out there
#

# FIXME The decision on which sort of release to download for a client needs to be put into the database
hostname=$(hostname)
flavor="PRODUCTION"
configFlavor=$(./getFlavor.sh)
if [ "$configFlavor X" != " X" ]; then
    flavor=$configFlavor;
fi

exitCode=0
if java gov.fnal.ppd.dd.util.version.VersionInformationComparison "$flavor"; then
    echo You are running the latest \"$flavor\" version of the software
    exitCode=1
else
    doTheUpdate=$yes;
    if [ $yes = 0 ]; then 
	details=$(java gov.fnal.ppd.dd.util.version.VersionInformation 2 $flavor)
	title="New $flavor version detected"
	text="The server has a newer version of the Dynamic Displays software.\nNew version details:\n\n$details\n\nShall we update to the newest version?";

	if zenity --question --width=900 --title="$title" --text="$text"; then 
	    doTheUpdate=1;
	fi
    fi

    log=log/update_${d}_$$.log

    if [ $doTheUpdate = 1 ]; then
	(
	    cd ../..
	    echo "5"
	    date > "$log"
	    echo Automatic software update >> "$log"
	    theVersion=$(java gov.fnal.ppd.dd.util.version.VersionInformation 4 | grep $flavor | awk '{ print $1 }')

	    if [ ! -e refreshSoftware.sh ]; then
		wget https://dynamicdisplays.fnal.gov/software/refreshSoftware.sh
		chmod +x refreshSoftware.sh
	    fi

	    ./refreshSoftware.sh "$theVersion" >> "$log" &
	    pid=$!

	    # A little trickery here to make the Zenity progress dialog look better.  
            # It takes about 80 seconds on an i3 NUC to get a new version from GIT (11/19/15)

	    for i in $(seq 10 2 90); do
		# Completing this loop will take 80 seconds
		sleep 2
		if kill -0 "$pid" 2>/dev/null ; then echo "$i"; else break; fi
	    done
	    echo "95"
	    wait "$pid" 2>/dev/null
	    echo "99"
	    echo "Dynamic Displays Software updated. $(date)"
	    echo "Automatic software update completed at $(date)" >> "$log"
	    ls -ltd roc* >> "$log"
	    sleep 1;
	    echo "100"
	    sleep 1;
	) |
	zenity --progress \
	    --title="Updating Dynamic Displays Software" \
	    --text="<span font-family=\"sans\" font-weight=\"900\" font-size=\"80000\">        Updating\nDynamic Displays\n       Software</span>" \
	    --auto-close \
	    --percentage=0
    fi

fi

echo
echo " Versioning -----------------------------------------------------------------"
echo
java gov.fnal.ppd.dd.util.version.VersionInformation 0
echo
echo " Versioning -----------------------------------------------------------------"

exit $exitCode
