@echo off

cd maven-mboot2

call .\build

cd ..

%JAVA_HOME%\bin\java -jar mboot.jar
