#!/bin/bash

for i in "fixFirefoxConfig.sh" "localJavaVersion.sh" "makeKeys.sh" "pushPullTest.sh" "runADisplay.sh" "runChannelListGUI.sh" "runDisplay.sh" "runIdentifyAll.sh" "runMessagingClient.sh" "runMessagingServer.sh" "runSelector.sh" "runStatusGUI.sh" "runVersionInformation.sh" "setupJars.sh" "startFirefoxOnly.sh" "tryKillLogShell.sh"; do
    echo "$i"
    shellcheck -x "$i"
done
