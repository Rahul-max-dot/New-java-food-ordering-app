
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

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin

                    echo "Current user:"
                    whoami

                    echo "Minikube executable:"
                    which minikube

                    echo "Minikube profile:"
                    minikube profile list

                    echo "Minikube status:"
                    minikube status -p minikube

                    echo "Kubernetes nodes:"
                    kubectl get nodes

                    echo "Building application image:"
                    minikube image build \
                      -p minikube \
                      -t ${IMAGE_NAME}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            agent { label 'minikube-agent' }

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin

                    echo "Checking Kubernetes connection:"
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "Applying Kubernetes deployment:"
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    echo "Updating application image:"
                    kubectl set image \
                      deployment/${DEPLOYMENT} \
                      ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    echo "Waiting for deployment:"
                    kubectl rollout status \
                      deployment/${DEPLOYMENT} \
                      --timeout=180s

                    echo "Application pods:"
                    kubectl get pods

                    echo "Application services:"
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

