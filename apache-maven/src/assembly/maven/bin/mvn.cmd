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

@REM Clear/define a variable for any options to be inserted via script
@REM We want to avoid trying to parse the external MAVEN_OPTS variable
SET INTERNAL_MAVEN_OPTS=

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

@REM Scan the arguments for version/quiet flags so that version-only
@REM invocations can be answered without starting Maven itself; the Java-17
@REM gate below then doubles as the settings probe used to render the banner.
set "IS_VERSION_AND_EXIT="
set "IS_SHOW_VERSION="
set "IS_QUIET="
set "IS_VERBOSE="
set "IS_MAIN_OVERRIDE="
for %%a in (%*) do (
  if "%%~a"=="-v" set "IS_VERSION_AND_EXIT=1"
  if "%%~a"=="--version" set "IS_VERSION_AND_EXIT=1"
  if "%%~a"=="-V" set "IS_SHOW_VERSION=1"
  if "%%~a"=="--show-version" set "IS_SHOW_VERSION=1"
  if "%%~a"=="-q" set "IS_QUIET=1"
  if "%%~a"=="--quiet" set "IS_QUIET=1"
  if "%%~a"=="-X" set "IS_VERBOSE=1"
  if "%%~a"=="--debug" set "IS_VERBOSE=1"
  if "%%~a"=="--enc" set "IS_MAIN_OVERRIDE=1"
  if "%%~a"=="--shell" set "IS_MAIN_OVERRIDE=1"
  if "%%~a"=="--up" set "IS_MAIN_OVERRIDE=1"
)

:chkMHome
set "MAVEN_HOME=%~dp0"
set "MAVEN_HOME=%MAVEN_HOME:~0,-5%"
if "%MAVEN_HOME%"=="" goto error

:checkMCmd
if not exist "%MAVEN_HOME%\bin\mvn.cmd" goto error

@REM ==== FAST VERSION PATH ====
@REM When only version info is requested, render the banner from a single
@REM lightweight JVM (-XshowSettings) without starting Maven itself.
set "FAST_VERSION=0"
set "VERSION_SETTINGS_TEMP="
if defined IS_VERSION_AND_EXIT goto tryFastVersion
if defined IS_SHOW_VERSION goto tryFastVersion
goto javaGate

:tryFastVersion
if defined IS_VERBOSE goto javaGate
if defined IS_MAIN_OVERRIDE goto javaGate
set "VERSION_SETTINGS_TEMP=%TEMP%\mvn-version-%RANDOM%-%RANDOM%.txt"
"%JAVACMD%" --enable-native-access=ALL-UNNAMED -XshowSettings:properties -version 2> "%VERSION_SETTINGS_TEMP%"
if ERRORLEVEL 1 (
  del "%VERSION_SETTINGS_TEMP%" 2>nul
  set "VERSION_SETTINGS_TEMP="
  goto javaGate
)
set "FAST_VERSION=1"
goto versionPrint

:javaGate
"%JAVACMD%" --enable-native-access=ALL-UNNAMED -version >nul 2>&1
if ERRORLEVEL 1 (
    echo Error: Apache Maven 4.x requires Java 17 or newer to run. >&2
    "%JAVACMD%" -version >&2
    echo Please upgrade your Java installation or set JAVA_HOME to point to a compatible JDK. >&2
    goto error
)
if "%FAST_VERSION%"=="1" goto versionPrint
goto fastVersionDone

:versionPrint
call :printFastVersion "%VERSION_SETTINGS_TEMP%"
if defined VERSION_SETTINGS_TEMP del "%VERSION_SETTINGS_TEMP%" 2>nul
if defined IS_VERSION_AND_EXIT goto end
set "MAVEN_VERSION_PRINTED=-Dmaven.version.printed=true"
:fastVersionDone

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

rem Initialize JVM_CONFIG_MAVEN_OPTS to empty to avoid inheriting from environment
set JVM_CONFIG_MAVEN_OPTS=

if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" goto endReadJvmConfig

rem Use Java source-launch mode (JDK 11+) to parse jvm.config
rem This avoids batch script parsing issues with special characters (pipes, quotes, @, etc.)
rem Java writes parsed output to a temp file; we read it with 'set /p' + input redirect.
rem
rem Why 'set /p <file' instead of 'for /f ... in (file)':
rem   - 'for /f' silently produces no output when the file is briefly locked
rem     (e.g. by Windows Defender real-time scanning), leaving JVM_CONFIG_MAVEN_OPTS empty
rem     and causing hard-to-diagnose Maven startup failures.
rem   - 'set /p <file' uses a transient input-redirect open with different sharing flags,
rem     making it far less susceptible to lock contention. If the file IS locked, it
rem     emits a visible error to stderr rather than failing silently.
rem
rem Why not capture stdout directly ('for /f ... in (`command`)')?
rem   - cmd.exe's child shell interprets special characters (pipes |, ampersands &, etc.)
rem     in the captured output, which breaks jvm.config values like
rem     -Dhttp.nonProxyHosts=de|*.de  (the core use case for JvmConfigParser).

set "JVM_CONFIG_TEMP=%TEMP%\mvn-jvm-config-%RANDOM%-%RANDOM%.txt"

rem Debug logging (set MAVEN_DEBUG_SCRIPT=1 to enable)
if defined MAVEN_DEBUG_SCRIPT (
  echo [DEBUG] Found .mvn\jvm.config file at: %MAVEN_PROJECTBASEDIR%\.mvn\jvm.config
  echo [DEBUG] Using temp file: %JVM_CONFIG_TEMP%
  echo [DEBUG] Running JvmConfigParser with Java: %JAVACMD%
  echo [DEBUG] Parser arguments: "%MAVEN_HOME%\bin\JvmConfigParser.java" "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" "%MAVEN_PROJECTBASEDIR%" "%JVM_CONFIG_TEMP%"
)

rem Run parser with output file as third argument - Java writes directly to file
"%JAVACMD%" "%MAVEN_HOME%\bin\JvmConfigParser.java" "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" "%MAVEN_PROJECTBASEDIR%" "%JVM_CONFIG_TEMP%"
set JVM_CONFIG_EXIT=%ERRORLEVEL%

if defined MAVEN_DEBUG_SCRIPT (
  echo [DEBUG] JvmConfigParser exit code: %JVM_CONFIG_EXIT%
)

rem Check if parser failed
if %JVM_CONFIG_EXIT% neq 0 (
  echo ERROR: Failed to parse .mvn/jvm.config file 1>&2
  echo   jvm.config path: %MAVEN_PROJECTBASEDIR%\.mvn\jvm.config 1>&2
  echo   Java command: %JAVACMD% 1>&2
  if exist "%JVM_CONFIG_TEMP%" (
    del "%JVM_CONFIG_TEMP%" 2>nul
  )
  exit /b 1
)

rem Read the output file using 'set /p' with input redirect (see comment above)
if exist "%JVM_CONFIG_TEMP%" (
  if defined MAVEN_DEBUG_SCRIPT (
    echo [DEBUG] Temp file contents:
    type "%JVM_CONFIG_TEMP%"
  )
  set /p JVM_CONFIG_MAVEN_OPTS=<"%JVM_CONFIG_TEMP%" 2>nul
  rem Retry once after a brief delay if the read failed (Windows Defender file lock)
  if not defined JVM_CONFIG_MAVEN_OPTS (
    if defined MAVEN_DEBUG_SCRIPT (
      echo [DEBUG] First read returned empty, retrying after delay...
    )
    ping -n 2 127.0.0.1 >nul 2>nul
    set /p JVM_CONFIG_MAVEN_OPTS=<"%JVM_CONFIG_TEMP%" 2>nul
  )
  del "%JVM_CONFIG_TEMP%" 2>nul
)

if defined MAVEN_DEBUG_SCRIPT (
  echo [DEBUG] Final JVM_CONFIG_MAVEN_OPTS: %JVM_CONFIG_MAVEN_OPTS%
)

:endReadJvmConfig

@REM do not let MAVEN_PROJECTBASEDIR end with a single backslash which would escape the double quote. This happens when .mvn at drive root.
if "_%MAVEN_PROJECTBASEDIR:~-1%"=="_\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%\"

if "%MAVEN_DEBUG_ADDRESS%"=="" set MAVEN_DEBUG_ADDRESS=localhost:8000

goto endHandleArgs
:handleArgs
if "%~1"=="--debug" (
    if "%MAVEN_DEBUG_OPTS%"=="" (
        set "MAVEN_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%MAVEN_DEBUG_ADDRESS%"
    )
) else if "%~1"=="--yjp" (
    if not exist "%YJPLIB%" (
        echo Error: Unable to autodetect the YJP library location. Please set YJPLIB variable >&2
        exit /b 1
    )
    set "INTERNAL_MAVEN_OPTS=-agentpath:%YJPLIB%=onexit=snapshot,onexit=memory,tracing,onlylocal %INTERNAL_MAVEN_OPTS%"
) else if "%~1"=="--enc" (
    set "MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenEncCling"
) else if "%~1"=="--shell" (
      set "MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenShellCling"
) else if "%~1"=="--up" (
      set "MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenUpCling"
)
exit /b 0

:processArgs
if "%~1"=="" exit /b 0
call :handleArgs %1
shift
goto processArgs

:endHandleArgs
call :processArgs %*

for %%i in ("%MAVEN_HOME%"\boot\plexus-classworlds-*) do set LAUNCHER_JAR="%%i"
set LAUNCHER_CLASS=org.codehaus.plexus.classworlds.launcher.Launcher
if "%MAVEN_MAIN_CLASS%"=="" @set MAVEN_MAIN_CLASS=org.apache.maven.cling.MavenCling

@REM Only pass MAVEN_ARGS for the default Maven build command (MavenCling),
@REM not for sub-commands like --up, --enc, or --shell which have their own options.
if not "%MAVEN_MAIN_CLASS%"=="org.apache.maven.cling.MavenCling" set "MAVEN_ARGS="

if defined MAVEN_DEBUG_SCRIPT (
  echo [DEBUG] Launching JVM with command:
  echo [DEBUG]   "%JAVACMD%" %INTERNAL_MAVEN_OPTS% %MAVEN_OPTS% %JVM_CONFIG_MAVEN_OPTS% %MAVEN_DEBUG_OPTS% --enable-native-access=ALL-UNNAMED -classpath %LAUNCHER_JAR% "-Dclassworlds.conf=%CLASSWORLDS_CONF%" "-Dmaven.home=%MAVEN_HOME%" "-Dmaven.mainClass=%MAVEN_MAIN_CLASS%" "-Dlibrary.jline.path=%MAVEN_HOME%\lib\jline-native" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %MAVEN_VERSION_PRINTED% %LAUNCHER_CLASS% %MAVEN_ARGS% %*
)

"%JAVACMD%" ^
  %INTERNAL_MAVEN_OPTS% ^
  %MAVEN_OPTS% ^
  %JVM_CONFIG_MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  --enable-native-access=ALL-UNNAMED ^
  -classpath %LAUNCHER_JAR% ^
  "-Dclassworlds.conf=%CLASSWORLDS_CONF%" ^
  "-Dmaven.home=%MAVEN_HOME%" ^
  "-Dmaven.mainClass=%MAVEN_MAIN_CLASS%" ^
  "-Dlibrary.jline.path=%MAVEN_HOME%\lib\jline-native" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %MAVEN_VERSION_PRINTED% ^
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

:printFastVersion
@REM Renders the Maven version banner without starting Maven itself.
@REM %1 = path to the java -XshowSettings dump file (may be empty when absent)
set "_SETTINGS=%~1"
set "_MVN_NAME="
set "_MVN_SHORT="
set "_MVN_VERSION="
set "_MVN_BUILD="
set "_VFILE=%MAVEN_HOME%\bin\maven.version.properties"
if exist "%_VFILE%" (
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"distributionName=" "%_VFILE%"') do set "_MVN_NAME=%%b"
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"distributionShortName=" "%_VFILE%"') do set "_MVN_SHORT=%%b"
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"version=" "%_VFILE%"') do set "_MVN_VERSION=%%b"
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"buildNumber=" "%_VFILE%"') do set "_MVN_BUILD=%%b"
)
if not defined _MVN_NAME set "_MVN_NAME=Apache Maven"
if not defined _MVN_SHORT set "_MVN_SHORT=Maven"
if not defined _MVN_VERSION set "_MVN_VERSION=<version unknown>"

set "_JAVA_VERSION="
set "_JAVA_VENDOR="
set "_JAVA_HOME="
set "_LANG="
set "_COUNTRY="
set "_ENCODING="
set "_TIMEZONE="
set "_OS_NAME="
set "_OS_VERSION="
set "_OS_ARCH="

if not defined _SETTINGS goto printFastVersionOutput
for /f "tokens=1,* delims==" %%a in ('findstr /c:"java.version =" "%_SETTINGS%"') do set "_JAVA_VERSION=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"java.vendor =" "%_SETTINGS%"') do set "_JAVA_VENDOR=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"java.home =" "%_SETTINGS%"') do set "_JAVA_HOME=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"user.language =" "%_SETTINGS%"') do set "_LANG=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"user.country =" "%_SETTINGS%"') do set "_COUNTRY=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"file.encoding =" "%_SETTINGS%"') do set "_ENCODING=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"user.timezone =" "%_SETTINGS%"') do set "_TIMEZONE=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"os.name =" "%_SETTINGS%"') do set "_OS_NAME=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"os.version =" "%_SETTINGS%"') do set "_OS_VERSION=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /c:"os.arch =" "%_SETTINGS%"') do set "_OS_ARCH=%%b"

@REM The "key = value" format adds one leading space after '='; strip it.
if defined _JAVA_VERSION set "_JAVA_VERSION=%_JAVA_VERSION:~1%"
if defined _JAVA_VENDOR set "_JAVA_VENDOR=%_JAVA_VENDOR:~1%"
if defined _JAVA_HOME set "_JAVA_HOME=%_JAVA_HOME:~1%"
if defined _LANG set "_LANG=%_LANG:~1%"
if defined _COUNTRY set "_COUNTRY=%_COUNTRY:~1%"
if defined _ENCODING set "_ENCODING=%_ENCODING:~1%"
if defined _TIMEZONE set "_TIMEZONE=%_TIMEZONE:~1%"
if defined _OS_NAME set "_OS_NAME=%_OS_NAME:~1%"
if defined _OS_VERSION set "_OS_VERSION=%_OS_VERSION:~1%"
if defined _OS_ARCH set "_OS_ARCH=%_OS_ARCH:~1%"

if not defined _JAVA_VERSION set "_JAVA_VERSION=<unknown Java version>"
if not defined _JAVA_VENDOR set "_JAVA_VENDOR=<unknown vendor>"
if not defined _JAVA_HOME set "_JAVA_HOME=<unknown runtime>"
if not defined _ENCODING set "_ENCODING=<unknown encoding>"
if defined _COUNTRY (
  set "_LOCALE=%_LANG%_%_COUNTRY%"
) else (
  set "_LOCALE=%_LANG%"
)
if not defined _LOCALE set "_LOCALE=<unknown>"

@REM Time zone: not in -XshowSettings; try TZ env, else Windows registry not portable.
if not defined _TIMEZONE (
  if defined TZ (set "_TIMEZONE=%TZ%") else (set "_TIMEZONE=unknown")
)

@REM OS family: derived from os.name (case-insensitive substring).
set "_OS_FAMILY="
echo %_OS_NAME% | findstr /i "windows" >nul 2>&1 && set "_OS_FAMILY=windows"
echo %_OS_NAME% | findstr /i "mac" >nul 2>&1 && set "_OS_FAMILY=mac"
if not defined _OS_FAMILY if defined _OS_NAME set "_OS_FAMILY=unix"

:printFastVersionOutput
if defined IS_QUIET (
  echo %_MVN_VERSION%
  exit /b 0
)
if defined _MVN_BUILD (
  echo %_MVN_NAME% %_MVN_VERSION% (%_MVN_BUILD%)
) else (
  echo %_MVN_NAME% %_MVN_VERSION%
)
echo %_MVN_SHORT% home: %MAVEN_HOME%
echo Java version: %_JAVA_VERSION%, vendor: %_JAVA_VENDOR%, runtime: %_JAVA_HOME%
echo Default locale: %_LOCALE%, platform encoding: %_ENCODING%, time zone: %_TIMEZONE%
echo OS name: "%_OS_NAME%", version: "%_OS_VERSION%", arch: "%_OS_ARCH%", family: "%_OS_FAMILY%"
exit /b 0