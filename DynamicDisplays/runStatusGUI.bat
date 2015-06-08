call config.cmd

java -Dddisplay.dbusername=%databaseUsername% ^
	 -Dddisplay.dbpassword=%databasePassword% ^
	 -Xmx512m gov.fnal.ppd.dd.DDSystemStatus



