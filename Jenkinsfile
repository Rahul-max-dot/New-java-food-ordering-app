
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

                sh '''
                    set -e

                    echo "=== Java Version ==="
                    java -version

                    echo "=== Maven Version ==="
                    mvn -version

                    echo "=== Build and Test Application ==="
                    mvn clean test package
                '''

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
                    set -e

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Agent Information ==="
                    hostname
                    whoami
                    pwd

                    echo "=== Verify Minikube Files ==="
                    ls -la /home/ec2-user/.minikube/profiles/minikube/
                    ls -l /home/ec2-user/.kube/config

                    echo "=== Check Minikube ==="
                    /usr/local/bin/minikube status -p minikube

                    echo "=== Check Kubernetes Nodes ==="
                    kubectl get nodes

                    echo "=== Build Application Image ==="
                    /usr/local/bin/minikube image build \
                        -p minikube \
                        -t food-ordering:${BUILD_NUMBER} .

                    echo "=== Verify Image ==="
                    /usr/local/bin/minikube image ls -p minikube
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
                    set -e

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Verify Kubernetes Connection ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Verify Kubernetes Manifests ==="
                    test -f k8s/deployment.yaml
                    test -f k8s/service.yaml

                    echo "=== Apply Deployment ==="
                    kubectl apply -f k8s/deployment.yaml

                    echo "=== Apply Service ==="
                    kubectl apply -f k8s/service.yaml

                    echo "=== Update Application Image ==="
                    kubectl set image \
                        deployment/${DEPLOYMENT} \
                        ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    echo "=== Wait for Deployment ==="
                    kubectl rollout status \
                        deployment/${DEPLOYMENT} \
                        --timeout=180s

                    echo "=== Check Pods ==="
                    kubectl get pods -o wide

                    echo "=== Check Services ==="
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
            echo 'FAILED: Check Console Output for the exact error.'
        }

        always {
            echo 'CI/CD pipeline execution completed.'
        }
    }
}
