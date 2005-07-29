@REM ----------------------------------------------------------------------------
@REM Copyright 2001-2004 The Apache Software Foundation.
@REM 
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM 
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM 
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM 

@ECHO OFF
@REM This process assumes that maven-core-it-verifier has been built.
@REM NOTE: for simplicity, only Windows NT/2000/XP is current supported
@REM This also assumes that M2_HOME and JAVA_HOME are set, which are verified in the bootstrap script only

"%JAVA_HOME%\bin\java.exe" -Dmaven.home="%M2_HOME%" -cp "..\maven-core-it-verifier\target\maven-core-it-verifier-1.0.jar" org.apache.maven.it.Verifier %*

