@echo off

set buildDir=target
set classesDir=%buildDir%\classes
set srcDir=src\main\java

if exist %classesDir% rmdir /S/Q %buildDir%
if exist %buildDir% rmdir /S/Q %buildDir%

mkdir %buildDir%
mkdir %classesDir%

dir /B /s %srcDir%\*.java >sources
%JAVA_HOME%\bin\javac -d %classesDir% @sources
del /F/Q sources

cd %classesDir% 
%JAVA_HOME%\bin\jar -cfm ..\mboot.jar ..\..\manifest.txt *.*
cd ..\..

copy %buildDir%\mboot.jar ..
