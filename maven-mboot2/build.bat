@echo off
 
set buildDir=target
  
rmdir /S/Q %buildDir%
   
mkdir %buildDir%
    
%JAVA_HOME%\bin\javac -d %buildDir% @sources.txt
     
cd %buildDir% 
%JAVA_HOME%\bin\jar -cfm ..\mboot.jar ..\manifest.txt *
cd ..
