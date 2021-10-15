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
:findBaseDirLoop
if exist "%WDIR%\.mvn" goto baseDirFound
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

set "jvmConfig=\.mvn\jvm.config"
if not exist "%MAVEN_PROJECTBASEDIR%%jvmConfig%" goto endReadAdditionalConfig

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
@endlocal & set JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

:endReadAdditionalConfig

