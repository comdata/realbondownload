pipeline {
    environment {
        registry = "comdata456/realbondownload"
        registryCredential = 'docker-hub-credentials'
    }

    agent {
        docker {
            image 'comdata456/maven-jdk-11-slim' 
            args '-v $HOME/.m2:/root/.m2 -v /root/.ssh:/root/.ssh -v /run/docker.sock:/run/docker.sock' 
        }
    }

/*  tools {
    maven 'Maven 3.6.2'
  }*/


    stages {

        //stage('Prepare') {
 		//    steps {
		//	    sh 'apt-get update'
		//	    sh 'apt-get install -y docker'
        //    }
        //}

/*        stage('Build') {
            steps {
                sh 'mvn package'
            }
        }*/
        
        stage('Make Container') {

            steps {
                script {
                    docker.build registry + ":$BUILD_NUMBER"
                }

                //sh "docker build -t comdata456/realbondownload:${env.BUILD_ID} ."
                //sh "docker tag comdata456/realbondownload:${env.BUILD_ID} comdata456/realbondownload:latest"
            }
        }
    }

    post {
        always {
            archive 'target/**/*.jar'
            junit 'target/**/*.xml'
            //cucumber '**/*.json'
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
