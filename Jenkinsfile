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

properties([buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME=='master'?'5':'1'))])

def buildOs = 'linux'
def buildJdk = '8'
def buildMvn = '3.6.3'
def runITsOses = ['linux']
def runITsJdks = ['8', '11', '17']
def runITsMvn = '3.6.3'
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
        def MAVEN_GOAL='verify'

        stage('Configure deploy') {
           if (env.BRANCH_NAME in ['master', 'maven-3.8.x', 'maven-3.9.x']){
               MAVEN_GOAL='deploy'
           }
        }

        stage('Build / Unit Test') {
            String jdkName = jenkinsEnv.jdkFromVersion(buildOs, buildJdk)
            String mvnName = jenkinsEnv.mvnFromVersion(buildOs, buildMvn)
            try {
                withEnv(["JAVA_HOME=${ tool "$jdkName" }",
                         "PATH+MAVEN=${ tool "$jdkName" }/bin:${tool "$mvnName"}/bin",
                         "MAVEN_OPTS=-Xms2g -Xmx4g -Djava.awt.headless=true"]) {                   
                    sh "mvn clean ${MAVEN_GOAL} -B -U -e -fae -V -Dmaven.test.failure.ignore -PversionlessMavenDist -Dmaven.repo.local=${WORK_DIR}/.repository"
                }
            } finally {
                junit testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml', allowEmptyResults: true
            }
            dir ('apache-maven/target') {
                stash includes: 'apache-maven-bin.zip', name: 'maven-dist'
            }
        }
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
                    echo "NODE_NAME = ${env.NODE_NAME}"
                    // on Windows, need a short path or we hit 256 character limit for paths
                    // using EXECUTOR_NUMBER guarantees that concurrent builds on same agent
                    // will not trample each other plus workaround for JENKINS-52657
                    dir(isUnix() ? 'test' : "c:\\mvn-it-${EXECUTOR_NUMBER}.tmp") {
                        def WORK_DIR=pwd()
                        def ITS_BRANCH = env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH :  env.BRANCH_NAME;
                        try {
                          echo "Checkout ITs from branch: ${ITS_BRANCH}"
                          checkout([$class: 'GitSCM',
                                  branches: [[name: ITS_BRANCH]],
                                  extensions: [[$class: 'CloneOption', depth: 1, noTags: true, shallow: true]],
                                  userRemoteConfigs: [[url: 'https://github.com/apache/maven-integration-testing.git']]])
                        } catch (Throwable e) {
                          echo "Failure checkout ITs branch: ${ITS_BRANCH} - fallback maven-3.9.x branch"
                          checkout([$class: 'GitSCM',
                                  branches: [[name: "maven-3.9.x"]],
                                  extensions: [[$class: 'CloneOption', depth: 1, noTags: true, shallow: true]],
                                  userRemoteConfigs: [[url: 'https://github.com/apache/maven-integration-testing.git']]])
                        }

                        if (isUnix()) {
                            sh "rm -rvf $WORK_DIR/apache-maven-dist.zip $WORK_DIR/it-local-repo"
                        } else {
                            bat "if exist it-local-repo rmdir /s /q it-local-repo"
                            bat "if exist apache-maven-dist.zip del /q apache-maven-dist.zip"
                        }
                        dir('dists') {
                          unstash 'maven-dist'
                        }
                        try {
                            withEnv(["JAVA_HOME=${ tool "$jdkName" }",
                                        "PATH+MAVEN=${ tool "$jdkName" }/bin:${tool "$mvnName"}/bin",
                                        "MAVEN_OPTS=-Xms2g -Xmx4g -Djava.awt.headless=true"]) {                                               
                                String cmd = "${runITscommand} -DmavenDistro=$WORK_DIR/dists/apache-maven-bin.zip -Dmaven.test.failure.ignore"
                                if (isUnix()) {
                                    sh 'df -hT'
                                    sh "${cmd}"
                                } else {
                                    bat 'wmic logicaldisk get size,freespace,caption'
                                    bat "${cmd}"
                                }
                            }
                        } finally {
                            // in ITs test we need only reports from test itself
                            // test projects can contain reports with tested failed builds
                            junit testResults: '**/core-it-suite/target/surefire-reports/*.xml,**/core-it-support/**/target/surefire-reports/*.xml', allowEmptyResults: true
                            archiveDirs(stageId, ['core-it-suite-logs':'core-it-suite/target/test-classes',
                                                  'core-it-suite-reports':'core-it-suite/target/surefire-reports'])
                            deleteDir() // clean up after ourselves to reduce disk space
                        }
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
    echo "[FAILURE-002] FlowInterruptedException ${e}"
    // this ambiguous condition means a user probably aborted
    if (e.causes.size() == 0) {
        currentBuild.result = "ABORTED"
    } else {
        currentBuild.result = "FAILURE"
    }
    throw e
} catch (hudson.AbortException e) {
    echo "[FAILURE-003] AbortException ${e}"
    // this ambiguous condition means during a shell step, user probably aborted
    if (e.getMessage().contains('script returned exit code 143')) {
        currentBuild.result = "ABORTED"
    } else {
        currentBuild.result = "FAILURE"
    }
    throw e
} catch (InterruptedException e) {
    echo "[FAILURE-004] ${e}"
    currentBuild.result = "ABORTED"
    throw e
} catch (Throwable e) {
    echo "[FAILURE-001] ${e}"
    currentBuild.result = "FAILURE"
    throw e
} finally {
    // notify completion
    stage("Notifications") {
        jenkinsNotify()      
    }    
}

def archiveDirs(stageId, archives) {
    archives.each { archivePrefix, pathToContent ->
        if (fileExists(pathToContent)) {
            zip(zipFile: "${archivePrefix}-${stageId}.zip", dir: pathToContent, archive: true)
        }
    }
}
