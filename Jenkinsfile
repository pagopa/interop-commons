void sbtAction(String task) {
  container('sbt-container') {
    sh '''
        echo "
        realm=Sonatype Nexus Repository Manager
        host=${NEXUS}
        user=${NEXUS_CREDENTIALS_USR}
        password=${NEXUS_CREDENTIALS_PSW}" > ~/.sbt/.credentials
        '''
    sh "sbt -Dsbt.log.noformat=true ${task}"
  }
} 

void updateGithubCommit(String status) {
  def token = '${GITHUB_PAT_PSW}'
  sh """
    curl --silent --show-error \
      "https://api.github.com/repos/pagopa/${REPO_NAME}/statuses/${GIT_COMMIT}" \
      --header "Content-Type: application/json" \
      --header "Authorization: token ${token}" \
      --request POST \
      --data "{\\"state\\": \\"${status}\\",\\"context\\": \\"Jenkins Continuous Integration\\", \\"description\\": \\"Build ${BUILD_DISPLAY_NAME}\\"}" &> /dev/null
  """
}

pipeline {
  agent { label 'sbt-template' }
  environment {
    GITHUB_PAT = credentials('github-pat')
    NEXUS = "${env.NEXUS}"
    NEXUS_CREDENTIALS = credentials('pdnd-nexus')
    MAVEN_REPO = "${env.MAVEN_REPO}"
    // GIT_URL has the shape git@github.com:pagopa/REPO_NAME.git so we extract from it
    REPO_NAME="""${sh(returnStdout:true, script: 'echo ${GIT_URL} | sed "s_https://github.com/pagopa/\\(.*\\)\\.git_\\1_g"')}""".trim()
  }
  stages {
      stage('Testing Library') {
        steps {
            updateGithubCommit 'pending'
            sbtAction 'test'
          }
        }
      stage('Publishing Library on Nexus') {
        when {
          anyOf {
            branch pattern: "[0-9]+\\.[0-9]+\\.x", comparator: "REGEXP"
            buildingTag()
          }
        }
        steps {
          sbtAction 'publish'
        }
      }
  }
  post {
    success { 
      updateGithubCommit 'success'
    }
    failure { 
      updateGithubCommit 'failure'
    }
  }
}