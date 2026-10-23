
pipeline {
    agent none

    environment {
        IMAGE_NAME = 'food-ordering'
        DEPLOYMENT = 'food-ordering'
        CONTAINER = 'food-ordering'
        MINIKUBE_PROFILE = 'minikube'
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
                unstash 'source-code'

                sh 'mvn clean test package'

                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true
                )
            }
        }

        stage('Build Image in Minikube') {
            agent { label 'minikube-agent' }

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user/.minikube'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user/.minikube
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin

                    echo "=== User ==="
                    whoami

                    echo "=== Minikube version ==="
                    minikube version

                    echo "=== Minikube profile ==="
                    minikube profile list

                    echo "=== Minikube status ==="
                    minikube status -p minikube

                    echo "=== Kubernetes nodes ==="
                    kubectl get nodes

                    echo "=== Build application image ==="
                    minikube image build \
                        -p minikube \
                        -t food-ordering:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            agent { label 'minikube-agent' }

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user/.minikube'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user/.minikube
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/home/ec2-user/.minikube/bin:/usr/local/bin:/usr/bin:/bin

                    echo "=== Kubernetes connection ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Apply Kubernetes manifests ==="
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    echo "=== Update application image ==="
                    kubectl set image \
                        deployment/food-ordering \
                        food-ordering=food-ordering:${BUILD_NUMBER}

                    echo "=== Wait for rollout ==="
                    kubectl rollout status \
                        deployment/food-ordering \
                        --timeout=180s

                    echo "=== Application pods ==="
                    kubectl get pods

                    echo "=== Application services ==="
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
            echo 'Pipeline failed. Check Console Output for the failing stage.'
        }

        always {
            echo 'CI/CD pipeline execution completed.'
        }
    }
}
