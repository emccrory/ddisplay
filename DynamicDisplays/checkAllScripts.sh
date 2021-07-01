#!/bin/bash

aFailure=0

# for i in "fixFirefoxConfig.sh"      "localJavaVersion.sh"   "makeKeys.sh"         "pushPullTest.sh"     \
#          "runSummarize.sh"          "checkDiskSpace.sh"     "property.sh"         "getFlavor.sh"        \
#          "runADisplay.sh"           "runChannelListGUI.sh"  "runDisplay.sh"       "checkAllScripts.sh"  \
# 	   "runMessagingClient.sh"    "runMessagingServer.sh" "runSelector.sh"      "runStatusGUI.sh"     \
# 	   "runVersionInformation.sh" "setupEnvironment.sh"   "startFirefoxOnly.sh" "tryKillLogShell.sh"; do
for i in *.sh; do
    echo "$i"
    if shellcheck -x "$i"; then continue; else aFailure=1; fi
done

# If there was a failure, then the execution of this entire script fails.
# This is the signal to which Jenkins pays attention.

exit $aFailure
