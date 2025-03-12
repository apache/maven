#!groovy

pipeline {
  agent none
  // save some io during the build
  options {
    skipDefaultCheckout()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    //buildDiscarder logRotator( numToKeepStr: '60' )
    disableRestartFromStage()
  }
  stages {
    stage("Parallel Stage") {
      parallel {

        stage("Build / Test - JDK17") {
          agent { node { label 'linux' } }
          steps {
              timeout(time: 210, unit: 'MINUTES') {
                  checkout scm
                  mavenBuild("jdk17", "", "3.9.9") // javadoc:javadoc
        //              recordIssues id: "analysis-jdk17", name: "Static Analysis jdk17", aggregatingResults: true, enabledForFailure: true,
        //                            tools: [mavenConsole(), java(), checkStyle(), errorProne(), spotBugs(), javaDoc()],
        //                            skipPublishingChecks: true, skipBlames: true
        //              recordCoverage id: "coverage-jdk17", name: "Coverage jdk17", tools: [[parser: 'JACOCO']], sourceCodeRetention: 'MODIFIED',
        //                             sourceDirectories: [[path: 'src/main/java'], [path: 'target/generated-sources/ee8']]
              }
          }
        }

        stage("Build / Test - JDK21") {
          agent { node { label 'linux' } }
          steps {
            timeout(time: 210, unit: 'MINUTES') {
              script {
                properties([buildDiscarder(logRotator(artifactNumToKeepStr: '5', numToKeepStr: env.BRANCH_NAME == 'master' ? '30' : '5'))])
              }
              checkout scm
              mavenBuild("jdk21", "-Dspotbugs.skip=true -Djacoco.skip=true", "maven3")
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
def mavenBuild(jdk, extraArgs, mvnVersion) {
  script {
    try {

      withEnv(["JAVA_HOME=${tool "$jdk"}",
               "MAVEN_OPTS=-Xms4G -Xmx4G -Djava.awt.headless=true"]) {
        sh "mvn --errors --batch-mode --show-version org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=${mvnVersion}"
        sh "./mvnw clean install -B -U -e -DskipTests -PversionlessMavenDist -V -DdistributionTargetDir=${WORK_DIR}/.apache-maven-master"
        sh "./mvnw package -DskipTests -e -B -V -Prun-its -Dmaven.repo.local=${WORKDIR}/.repository/cached"
        sh "./mvnw install -Dmaven.home=${WORK_DIR}/.apache-maven-master -e -B -V -Prun-its -Dmaven.repo.local=${WORKDIR}/.repository/local -Dmaven.repo.local.tail=${WORKDIR}/.repository/cached"
      }
    }
    finally {
      junit testResults: '**/target/surefire-reports/**/*.xml,**/target/invoker-reports/TEST*.xml', allowEmptyResults: true
    }
  }
}
// vim: et:ts=2:sw=2:ft=groovy
