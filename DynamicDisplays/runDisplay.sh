#!/bin/bash

# Should I run a full-on Dynamic Display, or should I run the super-simplified Firefox-only display?

. setupJars.sh

temp=temp$$
java gov.fnal.ppd.dd.db.GetDefaultContentForDisplay > $temp

displayType=`grep DISPLAYTYPE $temp | awk '{ print $2 }'`

if [ $displayType = "Regular" ]; then
    echo ./runDisplay.sh
else
    echo ./startFirefoxOnly.sh
fi

rm $temp
