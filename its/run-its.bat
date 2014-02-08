@REM How JvZ runs the ITs from a clean slate if it would be on Windows

mvn clean install -Prun-its,embedded -Dmaven.repo.local=%cd%/repo


suite.addTestSuite( MavenITmng5572ReactorPluginExtensionsTest.class  );