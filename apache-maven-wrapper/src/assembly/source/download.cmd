@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.

SET javaClass="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\MavenWrapperDownloader.java"
IF EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\MavenDownloader.class" goto :runDownloader

if "%MVNW_VERBOSE%" == "true" echo  - Compiling MavenWrapperDownloader.java ...

@REM Compiling the Java class
"%JAVA_HOME%\bin\javac" %javaClass%

:runDownloader
IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\MavenWrapperDownloader.class" goto endWrapper

if "%MVNW_VERBOSE%" == "true" echo  - Running MavenWrapperDownloader.class ...
@REM Running the downloader
"%JAVA_HOME%\bin\java" -cp "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" MavenWrapperDownloader "%MAVEN_PROJECTBASEDIR%"

:endWrapper
@REM End of extension

