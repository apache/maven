@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
IF NOT EXIST %LAUNCHER_JAR% goto downloadWrapper

if "%MVNW_VERBOSE%" == "true" ECHO Found %LAUNCHER_JAR%
goto endWrapper

:downloadWrapper
set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/maven-wrapper/${project.version}/maven-wrapper-${project.version}.jar"

if not "%MVNW_REPOURL%" == "" (
    SET DOWNLOAD_URL="%MVNW_REPOURL%/org/apache/maven/maven-wrapper/${project.version}/maven-wrapper-${project.version}.jar"
)
if "%MVNW_VERBOSE%" == "true" (
    echo Couldn't find %LAUNCHER_JAR%, downloading it ...
    echo Downloading from: %DOWNLOAD_URL%
)

powershell -Command "&{"^
    "$webclient = new-object System.Net.WebClient;"^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
    "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
    "}"^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DOWNLOAD_URL%', '%LAUNCHER_JAR%')"^
    "}"
if "%MVNW_VERBOSE%" == "true" (
    echo Finished downloading %LAUNCHER_JAR%
)

:endWrapper
@REM End of extension

