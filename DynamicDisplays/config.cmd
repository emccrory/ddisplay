if exist {"c:\Program Files (x86)\Java\jre7\bin"} (
	set path="c:\Program Files (x86)\Java\jre7\bin";%path%
) else if exist {"c:\Program Files\Java\jre7\bin"} (
	set path="c:\Program Files\Java\jre7\bin";%path%
) else (
	echo "Java is not installed in the places that are expected.  Exit."
)

set classpath=bin;lib/mysql-connector-java-5.0.3-bin.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar

set "databaseServer=mccrory.fnal.gov"
set "messagingServer=roc-w-11.fnal.gov"
