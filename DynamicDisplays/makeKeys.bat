@ECHO OFF
call config.cmd

SET /P dbclient=Enter the name of the database user:
IF "%dbclient%" == "" GOTO ERROR
ECHO Using %dbclient% for accessing the database

SET /P selname=Enter the name of your selector (see instructions):
IF "%selname%" == "" GOTO ERROR
ECHO Using "%selname%" for your selector name

java -Dddisplay.dbserver=%databaseServer% ^
     -Dddisplay.dbname=%databaseName% ^
	 gov.fnal.ppd.security.GenerateNewKeyPair Public.key Private.key "%selname%" %dbclient%

GOTO End

:Error
ECHO You did not enter the information properly.  Bye.
:End
