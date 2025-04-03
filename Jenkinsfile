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
          agent { node { label 'ubuntu' } }
          steps {
              timeout(time: 210, unit: 'MINUTES') {
                checkout scm
                mavenBuild("jdk_17_latest", "-Djacoco.skip=true")
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
              // jacoco is definitely too slow
              mavenBuild("jdk_21_latest", "") // "-Pjacoco jacoco-aggregator:report-aggregate-all"
              //              recordIssues id: "analysis-jdk17", name: "Static Analysis jdk17", aggregatingResults: true, enabledForFailure: true,
              //                            tools: [mavenConsole(), java(), checkStyle(), errorProne(), spotBugs(), javaDoc()],
              //                            skipPublishingChecks: true, skipBlames: true
              // recordCoverage id: "coverage-jdk21", name: "Coverage jdk21", tools: [[parser: 'JACOCO',pattern: 'target/site/jacoco-aggregate/jacoco.xml']],
              //    sourceCodeRetention: 'MODIFIED', sourceDirectories: [[path: 'src/main/java']]
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
               "PATH+MAVEN=${ tool "$jdk" }/bin:${tool "maven_3_latest"}/bin",
               "MAVEN_OPTS=-Xms4G -Xmx4G -Djava.awt.headless=true"]) {
        sh "cp .github/ci-extensions.xml ~/.m2/extensions.xml"
        sh "mvn --errors --batch-mode --show-version org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=4.0.0-rc-3"
        sh "./mvnw clean install -B -U -e -DskipTests -PversionlessMavenDist -V -DdistributionTargetDir=${env.WORKSPACE}/.apache-maven-master"
        // we use two steps so that we can cache artifacts downloaded from Maven Central repository
        // without installing any local artifacts to not pollute the cache
        sh "echo package Its"
        sh "./mvnw package -DskipTests -e -B -V -Prun-its -Dmaven.repo.local=${env.WORKSPACE}/.repository/cached"
        sh "echo run Its"
        sh "./mvnw install -Pci $extraArgs -Dmaven.home=${env.WORKSPACE}/.apache-maven-master -e -B -V -Prun-its -Dmaven.repo.local=${env.WORKSPACE}/.repository/local -Dmaven.repo.local.tail=${env.WORKSPACE}/.repository/cached"
      }
    }
    finally {
      junit testResults: '**/target/test-results-surefire/*.xml', allowEmptyResults: true
    }
  }
}
// vim: et:ts=2:sw=2:ft=groovy
