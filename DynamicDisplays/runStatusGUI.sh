. setupJars.sh

messagingServer="mccrory.fnal.gov"
databaseServer="mccrory.fnal.gov"

java -Dddisplay.messagingserver=$messagingServer \
     -Dddisplay.dbserver=$databaseServer \
     -Xmx512m gov.fnal.ppd.DDSystemStatus

