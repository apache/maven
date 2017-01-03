/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

properties([buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME=='master'?'10':'5'))])

try {
node('ubuntu') {
    stage 'Checkout'
    def MAVEN_BUILD=tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'
    def JAVA7_HOME=tool name: 'JDK 1.7 (latest)', type: 'hudson.model.JDK'
    dir('build') {
        checkout scm
        def WORK_DIR=pwd()
        stage 'Build / Unit Test'
        withEnv(["PATH+MAVEN=$MAVEN_BUILD/bin","PATH+JDK=$JAVA7_HOME/bin"]) {
            sh "mvn clean verify -B -U -e -fae -V -Dmaven.test.failure.ignore=true -Dmaven.repo.local=$WORK_DIR/.repository -DskipTests"
        }
        dir ('apache-maven/target') {
            sh "mv apache-maven-*-bin.zip apache-maven-dist.zip"
            stash includes: 'apache-maven-dist.zip', name: 'dist'
        }
    }
}

stage 'Integration Test'
parallel linuxJava7:{
        node('ubuntu') {
            def MAVEN_NIX_J7=tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'
            def JAVA_NIX_J7=tool name: 'JDK 1.7 (latest)', type: 'hudson.model.JDK'
            dir('test') {
                def WORK_DIR=pwd()
                git(url:'https://git-wip-us.apache.org/repos/asf/maven-integration-testing.git', branch: 'master')
                sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                unstash 'dist'
                withEnv(["PATH+MAVEN=$MAVEN_NIX_J7/bin","PATH+JDK=$JAVA_NIX_J7/bin"]) {
                    sh "mvn clean verify  -Prun-its -B -U -V -Dmaven.test.failure.ignore=true -Dmaven.repo.local=$WORK_DIR/it-local-repo -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    junit allowEmptyResults: true, testResults:'**/target/*-reports/*.xml'
                }
            }
        }
    },linuxJava8: {
        node('ubuntu') {
            def MAVEN_NIX_J8=tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'
            def JAVA_NIX_J8=tool name: 'JDK 1.8 (latest)', type: 'hudson.model.JDK'
            dir('test') {
                def WORK_DIR=pwd()
                git(url:'https://git-wip-us.apache.org/repos/asf/maven-integration-testing.git', branch: 'master')
                sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                unstash 'dist'
                withEnv(["PATH+MAVEN=$MAVEN_NIX_J8/bin","PATH+JDK=$JAVA_NIX_J8/bin"]) {
                    sh "mvn clean verify  -Prun-its -B -U -V -Dmaven.test.failure.ignore=true -Dmaven.repo.local=$WORK_DIR/it-local-repo -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    junit allowEmptyResults: true, testResults:'**/target/*-reports/*.xml'
                }
            }
        }
    }, winJava7: {
        node('Windows') {
            def MAVEN_WIN_J7=tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'
            def JAVA_WIN_J7=tool name: 'JDK 1.8 (latest)', type: 'hudson.model.JDK'
            dir('test') {
                def WORK_DIR=pwd()
                git(url:'https://git-wip-us.apache.org/repos/asf/maven-integration-testing.git', branch: 'master')
                bat "if exist it-local-repo rmdir /s /q it-local-repo"
                bat "if exist apache-maven-dist.zip /q apache-maven-dist.zip"
                unstash 'dist'
                withEnv(["Path+MAVEN=$MAVEN_WIN_J7\\bin","Path+JDK=$JAVA_WIN_J7\\bin"]) {
                    bat "set"
                    bat "mvn clean verify  -Prun-its -B -U -V -Dmaven.test.failure.ignore=true -Dmaven.repo.local=$WORK_DIR/it-local-repo -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    junit allowEmptyResults: true, testResults:'**/target/*-reports/*.xml'
                }
            }
        }
    }, winJava8: {
        node('Windows') {
            def MAVEN_WIN_J8=tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'
            def JAVA_WIN_J8=tool name: 'JDK 1.8 (latest)', type: 'hudson.model.JDK'
            dir('test') {
                def WORK_DIR=pwd()
                git(url:'https://git-wip-us.apache.org/repos/asf/maven-integration-testing.git', branch: 'master')
                bat "if exist it-local-repo rmdir /s /q it-local-repo"
                bat "if exist apache-maven-dist.zip /q apache-maven-dist.zip"
                unstash 'dist'
                withEnv(["Path+MAVEN=$MAVEN_WIN_J8\\bin","Path+JDK=$JAVA_WIN_J8\\bin"]) {
                    bat "set"
                    bat "mvn clean verify  -Prun-its -B -U -V -Dmaven.test.failure.ignore=true -Dmaven.repo.local=$WORK_DIR/it-local-repo -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    junit allowEmptyResults: true, testResults:'**/target/*-reports/*.xml'
                }
            }
        }
    }
} finally {
    node('ubuntu') {
        emailext body: "See ${env.BUILD_URL}", recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider']], replyTo: 'dev@maven.apache.org', subject: "Maven Jenkinsfile finished with ${currentBuild.result}", to: 'notifications@maven.apache.org'
    }
}
