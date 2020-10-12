#!/bin/bash

# Do we run two instances of the Display?  Not sure.
# We'll look for the two config files and modify them anyway.
. setupJars.sh

XUL1=$(ls -d ~/.mozilla/firefox/*.DynamicDisplay)
XUL2=$(s -d ~/.mozilla/firefox/*.DynamicDisplay2)

if [ -e "$XUL2/xulstore.json" ]; then
    dim=$(dpyinfo | grep dimensions | sed -r 's/^[^0-9]*([0-9]+x[0-9]+).*$/\1/')
    x2=$(echo "$dim" | sed 's/x/ /g' | awk '{ print $1 }')
    x=$((x2/2))
    y=$(echo "$dim" | sed 's/x/ /g' | awk '{ print $2 }')
    wid=$((x-20))
    hei=$((y-50))
    cp "$XUL2/xulstore.json" "$XUL2/xulstore.json_$$"
    if java gov.fnal.ppd.dd.testing.RewriteJsonConfigFileForFirefox "$XUL2/xulstore.json" "$x" 0 "$wid" "$hei" > temp_$$; then
	cp temp_$$ "$XUL2/xulstore.json"
	echo "$(date)" Rewrote Firefox xulstore.json config file in "$XUL2" for Display instance 2 at $x 0 $wid $hei
	cp "$XUL1/xulstore.json" "$XUL1/xulstore.json_$$"
	if java gov.fnal.ppd.dd.testing.RewriteJsonConfigFileForFirefox "$XUL1/xulstore.json" 0 0 $wid $hei > temp_$$; then
	    cp temp_$$ "$XUL1/xulstore.json"
	    echo "$(date)" Rewrote Firefox xulstore.json config file in "$XUL1" for Display instance 1 at 0 0 $wid $hei
	fi
    fi
else
    echo There is only one display
fi
