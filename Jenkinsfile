//common helper for invoking SBT tasks
void sbtAction(String task) {
  echo "Executing ${task} on SBT"
  sh '''
      echo "
      realm=Sonatype Nexus Repository Manager
      host=${NEXUS}
      user=${NEXUS_CREDENTIALS_USR}
      password=${NEXUS_CREDENTIALS_PSW}" > ~/.sbt/.credentials
     '''

  sh "sbt -Dsbt.log.noformat=true ${task}"
}

pipeline {

  agent none

  stages {
    stage('Test and Publish library') {
      agent { label 'sbt-template' }
      environment {
        NEXUS = "${env.NEXUS}"
        NEXUS_CREDENTIALS = credentials('pdnd-nexus')
        MAVEN_REPO = "${env.MAVEN_REPO}"
      }
      steps {
        container('sbt-container') {
          script {
            sbtAction 'test publish'
          }
        }
      }
    }

  }
}