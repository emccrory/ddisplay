#!/bin/bash

# for the display software, use the command line argument, "Y", to just do the update, no questions asked.
yes=0;
if [ "$1 X" = "Y X" ]; then
    yes=1;
fi

. setupJars.sh

#
# Evaluate if there is a newer version of the software out there
#

if java gov.fnal.ppd.dd.util.version.VersionInformationComparison; then
    if [ $yes = 0 ]; then
	echo You are running the latest version of the software
    fi
else
    if [ $yes = 0 ]; then 
	details=`java gov.fnal.ppd.dd.util.version.VersionInformation`
	title="New version detected"
	text="The server has a newer version of the software.\nNew version details:\n\n$details\n\nShall we update to the newest version?";

	if zenity --question --title="$title" --text="$text"; then 
	# Insert code HERE to fetch the newset version of the software and then restart
	    echo At this time, you have to do this update yourself.  Sorry for the misdirection here.
	    cd ../..
	    ./refreshSoftware.sh
	fi
    else
	echo New version detected and auto yes is on. You have to do this update yourself.  Sorry for the misdirection here.
	cd ../..
	./refreshSoftware.sh
	echo Dynamic Displays Software updated. `date`
    fi
fi
