/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

pipeline {
  agent none
  // save some io during the build
  options {
    skipDefaultCheckout()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    disableRestartFromStage()
  }
  stages {
    stage("Build / Test - JDK17") {
      agent { node { label 'ubuntu' } }
      steps {
        timeout(time: 210, unit: 'MINUTES') {
          checkout scm
          mavenBuild("jdk_17_latest", "")
          script {
            properties([buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: isDeployedBranch() ? '30' : '5'))])
            if (isDeployedBranch()) {
              withEnv(["JAVA_HOME=${tool "jdk_17_latest"}",
                       "PATH+MAVEN=${ tool "jdk_17_latest" }/bin:${tool "maven_3_latest"}/bin",
                       "MAVEN_OPTS=-Xms4G -Xmx4G -Djava.awt.headless=true"]) {
                sh "mvn clean deploy -DdeployAtEnd=true -B"
              }
            }
          }
        }
      }
    }
  }
}

boolean isDeployedBranch() {
  return env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'maven-4.0.x' || env.BRANCH_NAME == 'maven-3.9.x'
}

/**
 * To other developers, if you are using this method above, please use the following syntax.
 * By default this method does NOT execute ITs anymore, just "install".
 *
 * mavenBuild("<jdk>", "<profiles> <goals> <plugins> <properties>"
 *
 * @param jdk the jdk tool name (in jenkins) to use for this build
 * @param extraArgs extra command line args
 */
def mavenBuild(jdk, extraArgs) {
  script {
    try {
      withEnv(["JAVA_HOME=${tool "$jdk"}",
               "PATH+MAVEN=${tool "$jdk"}/bin:${tool "maven_3_latest"}/bin",
               "MAVEN_OPTS=-Xms4G -Xmx4G -Djava.awt.headless=true"]) {
        sh "mvn --errors --batch-mode --show-version org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=3.9.10"
        sh "echo run Its"
        sh "./mvnw -e -B -V install $extraArgs"
      }
    }
    finally {
      junit testResults: '**/target/test-results-surefire/*.xml', allowEmptyResults: true
    }
  }
}
