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
def buildJdk = '8'
def buildMvn = '3.5.4'
def runITsOses = ['linux', 'windows']
def runITsJdks = ['7', '8', '11']
def runITsMvn = '3.5.4'
def runITscommand = "mvn clean install -Prun-its,embedded -B -U -V" // -DmavenDistro=... -Dmaven.test.failure.ignore=true
def tests

try {

def osNode = jenkinsEnv.labelForOS(buildOs) 
node(jenkinsEnv.nodeSelection(osNode)) {
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

Map runITsTasks = [:]
for (String os in runITsOses) {
    for (def jdk in runITsJdks) {
        String osLabel = jenkinsEnv.labelForOS(os);
        String jdkName = jenkinsEnv.jdkFromVersion(os, "${jdk}")
        String mvnName = jenkinsEnv.mvnFromVersion(os, "${runITsMvn}")
        echo "OS: ${os} JDK: ${jdk} => Label: ${osLabel} JDK: ${jdkName}"

        String stageId = "${os}-jdk${jdk}"
        String stageLabel = "Run ITs ${os.capitalize()} Java ${jdk}"
        runITsTasks[stageId] = {
            node(jenkinsEnv.nodeSelection(osLabel)) {
                stage("${stageLabel}") {
                    // on Windows, need a short path or we hit 256 character limit for paths
                    // using EXECUTOR_NUMBER guarantees that concurrent builds on same agent
                    // will not trample each other plus workaround for JENKINS-52657
                    dir(isUnix() ? 'test' : "c:\\mvn-it-${EXECUTOR_NUMBER}.tmp") {
                        def WORK_DIR=pwd()
                        checkout tests
                        if (isUnix()) {
                            sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                        } else {
                            bat "if exist it-local-repo rmdir /s /q it-local-repo"
                            bat "if exist apache-maven-dist.zip del /q apache-maven-dist.zip"
                        }
                        unstash 'dist'
                        withMaven(jdk: jdkName, maven: mvnName, mavenLocalRepo:"${WORK_DIR}/it-local-repo", options:[
                            junitPublisher(ignoreAttachments: false)
                        ]) {
                            String cmd = "${runITscommand} -DmavenDistro=$WORK_DIR/apache-maven-dist.zip -Dmaven.test.failure.ignore=true"
                            if (stageId.endsWith('-jdk7')) {
                              // Java 7u80 has TLS 1.2 disabled by default: need to explicitly enable
                              cmd = "${cmd} -Dhttps.protocols=TLSv1.2"
                            }
                            
                            if (isUnix()) {
                                sh "${cmd}"
                            } else {
                                bat "${cmd}"
                            }
                        }
                        deleteDir() // clean up after ourselves to reduce disk space
                    }
                }
            }
        }
    }
}

// run the parallel ITs
parallel(runITsTasks)

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
