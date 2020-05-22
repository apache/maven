:chkMHome
set "MAVEN_HOME=%~dp0"
set "MAVEN_HOME=%MAVEN_HOME:~0,-5%"
if "%MAVEN_HOME%"=="" goto error

:checkMCmd
if not exist "%MAVEN_HOME%\bin\mvn.cmd" goto error

