
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
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Check Minikube profile ==="
                    minikube profile list

                    echo "=== Check Minikube status ==="
                    minikube status -p minikube

                    echo "=== Check Kubernetes node ==="
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
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                unstash 'source-code'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Verify Kubernetes access ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Apply Deployment ==="
                    kubectl apply -f k8s/deployment.yaml

                    echo "=== Apply Service ==="
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
            echo 'SUCCESS: Food Ordering application deployed to Minikube!'
        }

        failure {
            echo 'FAILED: Check the Console Output for the failing stage.'
        }

        always {
            echo 'CI/CD pipeline execution completed.'
        }
    }
}
