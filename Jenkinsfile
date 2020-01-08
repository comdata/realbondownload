pipeline {
    def app
    agent {
        docker {
            image 'maven:3.6.1-jdk-8-alpine' 
            args '-v $HOME/.m2:/root/.m2 -v /root/.ssh:/root/.ssh' 
        }
    }
    
    stages {
	

//        stage('Clone repository') {
            /* Let's make sure we have the repository cloned to our workspace */

//            checkout scm
//        }

        stage('Compile app') {
            steps {
            withMaven() {
                sh '$MVN_CMD -T 1C -B package'
            }
            }
        }

        stage('Build image') {
            /* This builds the actual image; synonymous to
            * docker build on the command line */
steps {
             docker.build("comdata456/realbondownload")
}
        }


        stage('Push image') {
            /* Finally, we'll push the image with two tags:
            * First, the incremental build number from Jenkins
            * Second, the 'latest' tag.
            * Pushing multiple tags is cheap, as all the layers are reused. */
        steps {

            docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                app.push("${env.BUILD_NUMBER}")
                app.push("latest")
            }
        }
        }
    }
}