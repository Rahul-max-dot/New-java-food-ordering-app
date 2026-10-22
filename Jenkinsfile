
pipeline {
    agent any

    environment {
        IMAGE_NAME = 'food-ordering'
        DEPLOYMENT = 'food-ordering'
        CONTAINER = 'food-ordering'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn clean test package'
            }
        }

        stage('Build Image in Minikube') {
            steps {
                sh '''
                    minikube image build \
                      -t ${IMAGE_NAME}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    kubectl config use-context minikube

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
    }
}
