call config.cmd

set "public=false"

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.virtualdisplaynumbers=true ^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Dddisplay.dbname=%databaseName% ^
	 -Dddisplay.dbusername=%databaseUsername% ^
	 -Dddisplay.dbpassword=%databasePassword% ^
     -Xmx512m gov.fnal.ppd.dd.MakeChannelSelector 

