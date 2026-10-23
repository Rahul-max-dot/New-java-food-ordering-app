
pipeline {
    agent none

    environment {
        IMAGE_NAME = 'food-ordering'
        DEPLOYMENT = 'food-ordering'
        CONTAINER = 'food-ordering'
    }

    stages {

        stage('Checkout') {
            agent { label 'built-in' }

            steps {
                checkout scm
                stash name: 'source-code',
                      includes: '**/*',
                      excludes: '.git/**'
            }
        }

        stage('Build and Test') {
            agent { label 'built-in' }

            steps {
                sh 'mvn clean test package'

                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
            }
        }

        stage('Build Image in Minikube') {
            agent { label 'minikube-agent' }

            steps {
                unstash 'source-code'

                sh '''
                    minikube status
                    minikube image build \
                      -t ${IMAGE_NAME}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            agent { label 'minikube-agent' }

            steps {
                sh '''
                    kubectl config use-context minikube
                    kubectl get nodes

                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    kubectl set image \
                      deployment/${DEPLOYMENT} \
                      ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    kubectl rollout status \
                      deployment/${DEPLOYMENT} \
                      --timeout=180s

                    kubectl get pods
                    kubectl get services
                '''
            }
        }
    }

    post {
        success {
            echo 'Successfully deployed to Minikube!'
        }

        failure {
            echo 'Pipeline failed. Check Console Output.'
        }

        always {
            echo 'CI/CD pipeline execution completed.'
        }
    }
}

