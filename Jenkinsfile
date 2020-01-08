pipeline {
  agent any

  tools {
    maven 'mvn-3.5.2'
  }

  stages {
    stage('Build') {
      steps {
        sh 'mvn package'
      }
    }
    
    stage('Make Container') {
      steps {
      sh "docker build -t comdata456/realbondownload:${env.BUILD_ID} ."
      sh "docker tag comdata456/realbondownload:${env.BUILD_ID} comdata456/realbondownload:latest"
      }
    }
    
    stage('Check Specification') {
      steps {
        sh "chmod o+w *"
        sh "docker-compose up --exit-code-from cucumber --build"
      }
    }
  }

  post {
    always {
      archive 'target/**/*.jar'
      junit 'target/**/*.xml'
      cucumber '**/*.json'
    }
    success {
      withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        sh "docker login -u ${USERNAME} -p ${PASSWORD}"
        sh "docker push comdata456/realbondownload:${env.BUILD_ID}"
        sh "docker push comdata456/realbondownload:latest"
      }
    }
  }
}
