#!/bin/bash

databaseServer="mccrory.fnal.gov"
loc="0"

. setupJars.sh

java -Dddisplay.messagingserver=$messagingServer \
     -Dddisplay.dbserver=$databaseServer \
     -Dddisplay.selector.location=$loc \
     -Xmx512m gov.fnal.ppd.IdentifyAll
