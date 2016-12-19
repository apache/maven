pipeline {
    
    tools {
        maven "maven-3"
    }

    agent label:"java"

    post {
        always {
            archive '**/apache-maven/targer/apache-mavent-*'
            junit '**/target/*-reports/*.xml'
        }
    }

    stages {
        stage('build') {
            steps {
                sh 'mvn clean verify'
            }
        }
    }
}
