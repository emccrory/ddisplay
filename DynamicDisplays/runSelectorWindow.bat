call config.cmd

java -Dddisplay.selector.inwindow=TRUE ^
     -Dddisplay.virtualdisplaynumbers=TRUE ^
     -Dddisplay.selector.public=FALSE ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Dddisplay.dbname=%databaseName% ^
	 -Dddisplay.dbusername=%databaseUsername% ^
	 -Dddisplay.dbpassword=%databasePassword% ^
     -Xmx1024m gov.fnal.ppd.dd.MakeChannelSelector 

