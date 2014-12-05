call config.cmd

java -Dddisplay.messagingserver=%server% ^
     -Dddisplay.dbserver=%server% ^
     -Xmx512m gov.fnal.ppd.IdentifyAll