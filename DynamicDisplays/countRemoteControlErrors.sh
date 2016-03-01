#!/bin/bash

cd ../../log

logFile=`ls -t display*.log | head -1`

grep '"error"' $logFile
