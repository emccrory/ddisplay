set path="c:\Program Files (x86)\Java\jre7\bin";"c:\Program Files\Java\jre7\bin";%path%

set classpath=bin;lib/mysql-connector-java-5.1.27.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar

set HOME=\Users\%USERNAME%.FERMI

:read
( 
set /p ds=
set /p dport=
set /p dn=
set /p du=
set /p dp=
) <%HOME%\keystore\credentials.txt

set "databaseServer=%ds%:%dport%"
set databaseName=%dn%
set databaseUsername=%du%
set databasePassword=%dp%


