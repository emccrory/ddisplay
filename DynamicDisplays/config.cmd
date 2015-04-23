set path="c:\Program Files (x86)\Java\jre7\bin";"c:\Program Files\Java\jre7\bin";%path%

set classpath=bin;lib/mysql-connector-java-5.1.27.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar

set "messagingServer=roc-w-11.fnal.gov"

@echo off
setLocal EnableDelayedExpansion
set i=0
for /F %%a in (%HOMEPATH%\keystore\credentials.txt) do (
   set /A i+=1
   set array[!i!]=%%a
)

set "databaseServer=!array[1]!:!array[2]!"
set "databaseName=!array[3]!"
set "databaseUsername=!array[4]!"
set "databasePassword=!array[5]!"

REM echo %databaseServer% %databaseName% %databaseUsername% %databasePassword%

