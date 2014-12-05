call config.cmd

set "public=false"
set "loc=1"

java -Dddisplay.selector.inwindow=TRUE^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.selector.location=%loc% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx512m gov.fnal.ppd.dd.ChannelSelector 

