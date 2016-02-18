#!/bin/bash

. setupJars.sh

while {
    java -Xmx1024m gov.fnal.ppd.dd.util.TestExitCode -1
    test $? -eq 255
}
do
    echo Exit code was -1 so we shall continue
done

echo Got an exit code that was not -1.  Bye.
