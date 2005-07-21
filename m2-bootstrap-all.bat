@REM ----------------------------------------------------------------------------
@REM Copyright 2001-2004 The Apache Software Foundation.
@REM 
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM 
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM 
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM 

@REM ----------------------------------------------------------------------------
@REM Maven2 Bootstrap Batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM M2_HOME - location of maven2's installed home dir
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a key stroke before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven2 bootstrap
@REM     e.g. to run in offline mode, use
@REM set MAVEN_OPTS=-Dmaven.online=false
@REM ----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM enable echoing my setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto end

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto chkMHome

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto end

:chkMHome
if not "%M2_HOME%"=="" goto init

echo.
echo ERROR: M2_HOME not found in your environment.
echo Please set the M2_HOME variable in your environment to match the
echo location of the Maven installation
echo.
goto end
@REM ==== END VALIDATION ====

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set MAVEN_CMD_LINE_ARGS=%*
goto endInit

@REM The 4NT Shell from jp software
:4NTArgs
set MAVEN_CMD_LINE_ARGS=%$
goto endInit

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of agruments (up to the command line limit, anyway).
set MAVEN_CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto endInit
set MAVEN_CMD_LINE_ARGS=%MAVEN_CMD_LINE_ARGS% %1
shift
goto Win9xApp

@REM Reaching here means variables are defined and arguments have been captured
:endInit
SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
SET MAVEN_OPTS=%MAVEN_OPTS% -Dmaven.home="%M2_HOME%"

@REM Build MBoot2
cd maven-mboot2

call .\build

@REM Build Maven2
cd ..

%MAVEN_JAVA_EXE% %MAVEN_OPTS% -jar mboot.jar %MAVEN_CMD_LINE_ARGS%

@REM I Really Don't want to be rebuilding these (Especially the reports) every time, but
@REM until we regularly push them to the repository and the integration tests rely on
@REM some of these plugins, there is no choice
echo
echo -----------------------------------------------------------------------
echo Rebuilding maven2 plugins
echo -----------------------------------------------------------------------
cd maven-plugins
@REM update the release info to ensure these versions get used in the integration tests
call m2 --no-plugin-registry --check-plugin-latest --batch-mode -DupdateReleaseInfo=true -e %MAVEN_CMD_LINE_ARGS% clean:clean install
cd ..

echo
echo -----------------------------------------------------------------------
echo Running integration tests
echo -----------------------------------------------------------------------
cd maven-core-it
call maven-core-it
cd ..

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set MAVEN_JAVA_EXE=
set MAVEN_CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec
@REM pause the batch file if MAVEN_BATCH_PAUSE is set to 'on'
if "%MAVEN_BATCH_PAUSE%" == "on" pause

