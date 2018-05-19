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

def buildOs = 'linux'
def buildJdk = '7'
def buildMvn = '3.5.0'
def tests
def CORE_IT_PROFILES='run-its,embedded'

try {

node(jenkinsEnv.labelForOS(buildOs)) {
    dir('build') {
        stage('Checkout') {
            checkout scm
        }

        def WORK_DIR=pwd()

        stage('Build / Unit Test') {
            String jdkName = jenkinsEnv.jdkFromVersion(buildOs, buildJdk)
            String mvnName = jenkinsEnv.mvnFromVersion(buildOs, buildMvn)
            withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/.repository", options:[
                artifactsPublisher(disabled: false),
                junitPublisher(ignoreAttachments: false),
                findbugsPublisher(disabled: false),
                openTasksPublisher(disabled: false),
                dependenciesFingerprintPublisher(),
                invokerPublisher(),
                pipelineGraphPublisher()
            ]) {
                sh "mvn clean verify -B -U -e -fae -V -Dmaven.test.failure.ignore=true"
            }
            dir ('apache-maven/target') {
                sh "mv apache-maven-*-bin.zip apache-maven-dist.zip"
                stash includes: 'apache-maven-dist.zip', name: 'dist'
            }
        }

        tests = resolveScm source: [$class: 'GitSCMSource', credentialsId: '', id: '_', remote: 'https://gitbox.apache.org/repos/asf/maven-integration-testing.git', traits: [[$class: 'jenkins.plugins.git.traits.BranchDiscoveryTrait'], [$class: 'GitToolSCMSourceTrait', gitTool: 'Default']]], targets: [BRANCH_NAME, 'master']
    }
}


parallel linuxJava7:{
        node(jenkinsEnv.labelForOS('linux')) {
            stage ('Run ITs Linux Java 7') {
                String jdkName = jenkinsEnv.jdkFromVersion('linux', '7')
                String mvnName = jenkinsEnv.mvnFromVersion('linux', buildMvn)
                dir('test') {
                    def WORK_DIR=pwd()
                    checkout tests
                    sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                    unstash 'dist'
                    withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/it-local-repo", options:[
                        junitPublisher(ignoreAttachments: false)
                    ]) {
                        sh "mvn clean install -P$CORE_IT_PROFILES -B -U -V -Dmaven.test.failure.ignore=true -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    }
                    deleteDir() // clean up after ourselves to reduce disk space
                }
            }
        }
    },linuxJava8: {
        node(jenkinsEnv.labelForOS('linux')) {
            stage ('Run ITs Linux Java 8') {
                String jdkName = jenkinsEnv.jdkFromVersion('linux', '8')
                String mvnName = jenkinsEnv.mvnFromVersion('linux', buildMvn)
                dir('test') {
                    def WORK_DIR=pwd()
                    checkout tests
                    sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                    unstash 'dist'
                    withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/it-local-repo", options:[
                        junitPublisher(ignoreAttachments: false)
                    ]) {
                        sh "mvn clean install -P$CORE_IT_PROFILES -B -U -V -Dmaven.test.failure.ignore=true -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    }
                    deleteDir() // clean up after ourselves to reduce disk space
                }
            }
        }
    }, winJava7: {
        node(jenkinsEnv.labelForOS('windows')) {
            stage ('Run ITs Windows Java 7') {
                String jdkName = jenkinsEnv.jdkFromVersion('windows', '7')
                String mvnName = jenkinsEnv.mvnFromVersion('windows', buildMvn)

                // need a short path or we hit 256 character limit for paths
                // using EXECUTOR_NUMBER guarantees that concurrent builds on same agent
                // will not trample each other
                dir("/mvn-it-${EXECUTOR_NUMBER}.tmp") {
                    def WORK_DIR=pwd()
                    checkout tests
                    bat "if exist it-local-repo rmdir /s /q it-local-repo"
                    bat "if exist apache-maven-dist.zip del /q apache-maven-dist.zip"
                    unstash 'dist'
                    withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/it-local-repo", options:[
                        junitPublisher(ignoreAttachments: false)
                    ]) {
                        bat "mvn clean install -P$CORE_IT_PROFILES -B -U -V -Dmaven.test.failure.ignore=true -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    }
                    deleteDir() // clean up after ourselves to reduce disk space
                }
            }
        }
    }, winJava8: {
        node(jenkinsEnv.labelForOS('windows')) {
            stage ('Run ITs Windows Java 8') {
                String jdkName = jenkinsEnv.jdkFromVersion('windows', '8')
                String mvnName = jenkinsEnv.mvnFromVersion('windows', buildMvn)

                // need a short path or we hit 256 character limit for paths
                // using EXECUTOR_NUMBER guarantees that concurrent builds on same agent
                // will not trample each other
                dir("/mvn-it-${EXECUTOR_NUMBER}.tmp") {
                    def WORK_DIR=pwd()
                    checkout tests
                    bat "if exist it-local-repo rmdir /s /q it-local-repo"
                    bat "if exist apache-maven-dist.zip del /q apache-maven-dist.zip"
                    unstash 'dist'
                    withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/it-local-repo", options:[
                        junitPublisher(ignoreAttachments: false)
                    ]) {
                        bat "mvn clean install -P$CORE_IT_PROFILES -B -U -V -Dmaven.test.failure.ignore=true -DmavenDistro=$WORK_DIR/apache-maven-dist.zip"
                    }
                    deleteDir() // clean up after ourselves to reduce disk space
                }
            }
        }
    }

// JENKINS-34376 seems to make it hard to detect the aborted builds
} catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
    // this ambiguous condition means a user probably aborted
    if (e.causes.size() == 0) {
        currentBuild.result = "ABORTED"
    } else {
        currentBuild.result = "FAILURE"
    }
    throw e
} catch (hudson.AbortException e) {
    // this ambiguous condition means during a shell step, user probably aborted
    if (e.getMessage().contains('script returned exit code 143')) {
        currentBuild.result = "ABORTED"
    } else {
        currentBuild.result = "FAILURE"
    }
    throw e
} catch (InterruptedException e) {
    currentBuild.result = "ABORTED"
    throw e
} catch (Throwable e) {
    currentBuild.result = "FAILURE"
    throw e
} finally {
    // notify completion
    stage("Notifications") {
        jenkinsNotify()      
    }    
}
