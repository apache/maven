@echo off

set buildDir=target
set classesDir=%buildDir%\classes
set srcDir=src\main\java

if exist %classesDir% rmdir /S/Q %buildDir%
if exist %buildDir% rmdir /S/Q %buildDir%

mkdir %buildDir%
mkdir %classesDir%

%JAVA_HOME%\bin\javac -d %classesDir% %srcDir%\*.java

cd %classesDir% 
%JAVA_HOME%\bin\jar -cfm ..\mboot.jar ..\..\manifest.txt *.*
cd ..\..

copy %buildDir%\mboot.jar ..
