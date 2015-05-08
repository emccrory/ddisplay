call config.cmd

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.virtualdisplaynumbers=TRUE ^
     -Dddisplay.selector.public=FALSE ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Dddisplay.dbname=%databaseName% ^
	 -Dddisplay.dbusername=%databaseUsername% ^
	 -Dddisplay.dbpassword=%databasePassword% ^
     -Xmx512m gov.fnal.ppd.dd.MakeChannelSelector 

