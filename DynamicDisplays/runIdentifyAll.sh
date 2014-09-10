#!/bin/bash

. setupJars.sh

java -Dddisplay.selector.location=0 -Xmx512m gov.fnal.ppd.IdentifyAll
