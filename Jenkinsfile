#!groovy

pipeline {
  agent none
  // save some io during the build
  options {
    skipDefaultCheckout()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    disableRestartFromStage()
  }
  stages {
    stage("Parallel Stage") {
      parallel {

        stage("Build / Test - JDK17") {
          agent { node { label 'ubuntu' } }
          steps {
              timeout(time: 210, unit: 'MINUTES') {
                checkout scm
                mavenBuild("jdk_17_latest", "")
                script {
                  properties([buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'master' ? '30' : '5'))])
                  if (env.BRANCH_NAME == 'master') {
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

        stage("Build / Test - JDK21") {
          agent { node { label 'ubuntu' } }
          steps {
            timeout(time: 210, unit: 'MINUTES') {
              checkout scm
              mavenBuild("jdk_21_latest", "")
            }
          }
        }
      }
    }
  }
}

/**
 * To other developers, if you are using this method above, please use the following syntax.
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
        sh "./mvnw install $extraArgs -e -B -V -Prun-its"
      }
    }
    finally {
      junit testResults: '**/target/test-results-surefire/*.xml', allowEmptyResults: true
    }
  }
}
