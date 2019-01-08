@ECHO OFF

call config.cmd


SET dbclient=insertpublickey

REM SET /P dbclient=Enter the name of the database user:


IF "%dbclient%" == "" GOTO ERROR

ECHO Using %dbclient% for accessing the database



for /f "delims=" %%a in ('hostname') do @set myHost=%%a

SET selname=%myHost% selector 00

REM SET /P selname=Enter the name of your selector (see instructions):


IF "%selname%" == "" GOTO ERROR

ECHO Using "%selname%" for your selector name



echo java -Dddisplay.dbserver=%databaseServer% -Dddisplay.dbname=%databaseName% gov.fnal.ppd.security.GenerateNewKeyPair "%selname%" %dbclient%


 java -Dddisplay.dbserver=%databaseServer% -Dddisplay.dbname=%databaseName% gov.fnal.ppd.security.GenerateNewKeyPair "%selname%" %dbclient%



GOTO End


:Error
ECHO You did not enter the information properly.  Bye.


:End
