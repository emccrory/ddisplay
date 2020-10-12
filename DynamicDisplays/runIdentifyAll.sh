#!/bin/bash

databaseServer="mccrory.fnal.gov"
loc="0"

. setupJars.sh

java -Dddisplay.dbserver="$databaseServer" \
     -Dddisplay.selector.location="$loc" \
     -Xmx512m gov.fnal.ppd.dd.IdentifyAll
