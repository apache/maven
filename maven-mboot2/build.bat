@echo off

set buildDir=target

if exist %buildDir% rmdir /S/Q %buildDir%

mkdir %buildDir%

%JAVA_HOME%\bin\javac -d %buildDir% @sources.txt

cd %buildDir% 
%JAVA_HOME%\bin\jar -cfm ..\mboot.jar ..\manifest.txt *
cd ..

cp mboot.jar ..
