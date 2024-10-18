@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@REM -----------------------------------------------------------------------------
@REM Apache Maven Startup Script
@REM
@REM Environment Variable Prerequisites
@REM
@REM   JAVA_HOME         (Optional) Points to a Java installation.
@REM   MAVEN_ARGS        (Optional) Arguments passed to Maven before CLI arguments.
@REM   MAVEN_BATCH_ECHO  (Optional) Set to 'on' to enable the echoing of the batch commands.
@REM   MAVEN_BATCH_PAUSE (Optional) set to 'on' to wait for a key stroke before ending.
@REM   MAVEN_OPTS        (Optional) Java runtime options used when Maven is executed.
@REM   MAVEN_SKIP_RC     (Optional) Flag to disable loading of mavenrc files.
@REM -----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM set title of command window
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@REM Execute a user defined script before this one
if not "%MAVEN_SKIP_RC%"=="" goto skipRc
if exist "%PROGRAMDATA%\mavenrc.cmd" call "%PROGRAMDATA%\mavenrc.cmd" %*
@REM check for pre script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_pre.bat" echo Warning: The mavenrc_pre.bat script is deprecated and will be removed in a future version. >&2
if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat" %*
if exist "%USERPROFILE%\mavenrc_pre.cmd" echo Warning: The mavenrc_pre.cmd script is deprecated and will be removed in a future version. >&2
if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd" %*
if exist "%USERPROFILE%\mavenrc.cmd" call "%USERPROFILE%\mavenrc.cmd" %*
:skipRc

@setlocal

set ERROR_CODE=0

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto javaHomeSet
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJavaCmd

:javaHomeSet
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVACMD%" (
  echo The JAVA_HOME environment variable is not defined correctly, so Apache Maven cannot be started. >&2
  echo JAVA_HOME is set to "%JAVA_HOME%", but "%%JAVA_HOME%%\bin\java.exe" does not exist. >&2
  goto error
)

:checkJavaCmd
if not exist "%JAVACMD%" (
  echo The java.exe command does not exist in PATH nor is JAVA_HOME set, so Apache Maven cannot be started. >&2
  goto error
)
:chkMHome
set "MAVEN_HOME=%~dp0"
set "MAVEN_HOME=%MAVEN_HOME:~0,-5%"
if "%MAVEN_HOME%"=="" goto error

:checkMCmd
if not exist "%MAVEN_HOME%\bin\mvn.cmd" goto error

@REM ==== END VALIDATION ====

:init

set "CLASSWORLDS_CONF=%MAVEN_HOME%\bin\m2.conf"

@REM Find the project basedir, i.e., the directory that contains the directory ".mvn".
@REM Fallback to current working directory if not found.

set "EXEC_DIR=%CD%"
set "WDIR=%EXEC_DIR%"

@REM Look for the --file switch and start the search for the .mvn directory from the specified
@REM POM location, if supplied.

set FILE_ARG=
:arg_loop
if "%~1" == "-f" (
  set "FILE_ARG=%~2"
  shift
  goto process_file_arg
)
if "%~1" == "--file" (
  set "FILE_ARG=%~2"
  shift
  goto process_file_arg
)
@REM If none of the above, skip the argument
shift
if not "%~1" == "" (
  goto arg_loop
) else (
  goto findBaseDir
)

:process_file_arg
if "%FILE_ARG%" == "" (
  goto findBaseDir
)
if not exist "%FILE_ARG%" (
  echo POM file "%FILE_ARG%" specified the -f/--file command-line argument does not exist >&2
  goto error
)
if exist "%FILE_ARG%\*" (
  set "POM_DIR=%FILE_ARG%"
) else (
  call :get_directory_from_file "%FILE_ARG%"
)
if not exist "%POM_DIR%" (
  echo Directory "%POM_DIR%" extracted from the -f/--file command-line argument "%FILE_ARG%" does not exist >&2
  goto error
)
set "WDIR=%POM_DIR%"
goto findBaseDir

:get_directory_from_file
set "POM_DIR=%~dp1"
:stripPomDir
if not "_%POM_DIR:~-1%"=="_\" goto pomDirStripped
set "POM_DIR=%POM_DIR:~0,-1%"
goto stripPomDir
:pomDirStripped
exit /b

:findBaseDir
cd /d "%WDIR%"
set "WDIR=%CD%"
:findBaseDirLoop
if exist ".mvn" goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set "WDIR=%CD%"
goto findBaseDirLoop

:baseDirFound
set "MAVEN_PROJECTBASEDIR=%WDIR%"
cd /d "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
if "_%EXEC_DIR:~-1%"=="_\" set "EXEC_DIR=%EXEC_DIR:~0,-1%"
set "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
cd /d "%EXEC_DIR%"

:endDetectBaseDir

if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" goto endReadJvmConfig

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_OPTS=!JVM_CONFIG_MAVEN_OPTS! %%a
@endlocal & set MAVEN_OPTS=%MAVEN_OPTS% %JVM_CONFIG_MAVEN_OPTS%

:endReadJvmConfig

@REM do not let MAVEN_PROJECTBASEDIR end with a single backslash which would escape the double quote. This happens when .mvn at drive root.
if "_%MAVEN_PROJECTBASEDIR:~-1%"=="_\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%\"

if "%MAVEN_DEBUG_ADDRESS%"=="" set MAVEN_DEBUG_ADDRESS=localhost:8000

goto endHandleArgs
:handleArgs
:handleArgsLoop
if "%~1"=="" goto endHandleArgs
if "%~1"=="--debug" (
    if "%MAVEN_DEBUG_OPTS%"=="" (
        set "MAVEN_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%MAVEN_DEBUG_ADDRESS%"
    )
) else if "%~1"=="--yjp" (
    if not exist "%YJPLIB%" (
        echo Error: Unable to autodetect the YJP library location. Please set YJPLIB variable >&2
        exit /b 1
    )
    set "MAVEN_OPTS=-agentpath:%YJPLIB%=onexit=snapshot,onexit=memory,tracing,onlylocal %MAVEN_OPTS%"
) else if "%~1"=="--enc" (
    set "MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenEncCling"
)
shift
goto handleArgsLoop
:endHandleArgs

call :handleArgs %*

for %%i in ("%MAVEN_HOME%"\boot\plexus-classworlds-*) do set LAUNCHER_JAR="%%i"
set LAUNCHER_CLASS=org.codehaus.plexus.classworlds.launcher.Launcher
if "%MAVEN_MAIN_CLASS%"=="" @set MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenCling

"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  --enable-native-access=ALL-UNNAMED ^
  -classpath %LAUNCHER_JAR% ^
  "-Dclassworlds.conf=%CLASSWORLDS_CONF%" ^
  "-Dmaven.home=%MAVEN_HOME%" ^
  "-Dmaven.mainClass=%MAVEN_MAIN_CLASS%" ^
  "-Dlibrary.jansi.path=%MAVEN_HOME%\lib\jansi-native" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %LAUNCHER_CLASS% ^
  %MAVEN_ARGS% ^
  %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_post.bat" echo Warning: The mavenrc_post.bat script is deprecated and will be removed in a future version. >&2
if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
if exist "%USERPROFILE%\mavenrc_post.cmd" echo Warning: The mavenrc_post.cmd script is deprecated and will be removed in a future version. >&2
if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

@REM pause the script if MAVEN_BATCH_PAUSE is set to 'on'
if "%MAVEN_BATCH_PAUSE%"=="on" pause

exit /b %ERROR_CODE%
