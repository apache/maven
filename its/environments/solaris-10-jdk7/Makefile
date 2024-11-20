# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
.PHONY: default
default: build

clean:
	rm -f jdk-7u80-solaris-i586.tar.Z jdk-7u80-solaris-x64.tar.Z apache-maven-3.3.9-bin.tar.gz

jdk-7u80-solaris-i586.tar.Z:
	wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u80-b15/jdk-7u80-solaris-i586.tar.Z

jdk-7u80-solaris-x64.tar.Z:
	wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u80-b15/jdk-7u80-solaris-x64.tar.Z

apache-maven-3.3.9-bin.tar.gz:
	wget --no-check-certificate https://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz

build: jdk-7u80-solaris-i586.tar.Z jdk-7u80-solaris-x64.tar.Z apache-maven-3.3.9-bin.tar.gz
