call config.cmd

set "public=false"
set "loc=3"

java -Dddisplay.selector.inwindow=FALSE^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.selector.location=%loc% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx1024m gov.fnal.ppd.dd.ChannelSelector 

