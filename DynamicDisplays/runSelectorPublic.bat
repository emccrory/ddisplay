call config.cmd

set "public=true"

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx512m gov.fnal.ppd.dd.MakeChannelSelector 

