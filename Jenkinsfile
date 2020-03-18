pipeline {
    environment {
        registry = "comdata456/realbondownload"
        registryCredential = 'docker-hub-credentials'
    }

    agent {
        docker {
            image 'maven' 
            args '-v $HOME/.m2:/root/.m2 -v /root/.ssh:/root/.ssh -v /run/docker.sock:/run/docker.sock -v /usr/bin/docker:/usr/bin/docker' 
        }
    }



    stages {


        stage('Build') {
            steps {
                sh 'mvn package'
            }
        }
        
        stage('Make Container') {

            steps {
                sh "/usr/bin/docker build -t comdata456/realbondownload:${env.BUILD_ID} ."
                sh "/usr/bin/docker tag comdata456/realbondownload:${env.BUILD_ID} comdata456/realbondownload:latest"
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/**/*.jar', fingerprint: true
        }
        success {
            withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                sh "/usr/bin/docker login -u ${USERNAME} -p ${PASSWORD}"
                sh "/usr/bin/docker push comdata456/realbondownload:${env.BUILD_ID}"
                sh "/usr/bin/docker push comdata456/realbondownload:latest"
            }
        }
    }
}
