call config.cmd

set "public=true"

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx1024m gov.fnal.ppd.dd.MakeChannelSelector 

