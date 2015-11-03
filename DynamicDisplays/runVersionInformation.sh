#!/bin/bash

. setupJars.sh

#
# Evaluate if there is a newer version of the software out there
#

if java gov.fnal.ppd.dd.util.version.VersionInformationComparison; then
    echo You are running the latest version of the software
else
    title="New version detected"
    text="There exists a newer version of the software than the version we are about to run.\n\nShall we update to the newest version?";

    if zenity --question --title=$title --text=$text; then 
	# Insert code HERE to fetch the newset version of the software and then restart
	echo At this time, you have to do this update yourself.  Sorry for the misdirection here.
    fi

fi
