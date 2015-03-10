REM This is the same script as runSelector.bat now

call config.cmd

set "public=false"

java -Dddisplay.selector.inwindow=TRUE^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx512m gov.fnal.ppd.dd.MakeChannelSelector 



