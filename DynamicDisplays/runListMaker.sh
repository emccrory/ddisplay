. setupJars.sh


java -Dddisplay.selector.inwindow=$window \
     -Dddisplay.selector.public=$public \
     -Dddisplay.dbserver=$databaseServer \
     -Dddisplay.dbusername=$databaseUsername \
     -Dddisplay.dbpassword=$databasePassword \
     -Dddisplay.virtualdisplaynumbers=TRUE \
     -Xmx512m  gov.fnal.ppd.dd.channel.list.CreateListOfChannels
