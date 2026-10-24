
pipeline {
    agent none

    environment {
        IMAGE_NAME = 'food-ordering'
        DEPLOYMENT = 'food-ordering'
        CONTAINER = 'food-ordering'
        MINIKUBE_PROFILE = 'minikube'

        AWS_REGION = 'ap-southeast-2'
        CLOUDWATCH_NAMESPACE = 'FoodOrdering/CI-CD'
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

                    echo "=== Build and Test ==="
                    mvn clean test package
                '''

                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true
                )
            }
        }

        stage('Check Minikube Agent') {
            agent { label 'minikube-agent' }

            steps {
                sh '''
                    echo "=== Agent Hostname ==="
                    hostname

                    echo "=== Agent User ==="
                    whoami

                    echo "=== Home Directory ==="
                    echo "HOME=$HOME"
                    pwd

                    echo "=== Minikube Directory ==="
                    ls -ld /home/ec2-user/.minikube || true

                    echo "=== Minikube Profile ==="
                    ls -l /home/ec2-user/.minikube/profiles/minikube/config.json || true

                    echo "=== Kubernetes Config ==="
                    ls -l /home/ec2-user/.kube/config || true

                    echo "=== Find Profile ==="
                    find /home /root \
                        -path '*/profiles/minikube/config.json' \
                        2>/dev/null || true
                '''
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

                    echo "=== Verify Minikube Configuration ==="
                    test -f /home/ec2-user/.minikube/profiles/minikube/config.json
                    test -f /home/ec2-user/.kube/config

                    echo "=== Minikube Status ==="
                    /usr/local/bin/minikube status -p minikube

                    echo "=== Kubernetes Nodes ==="
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

                    echo "=== Kubernetes Connection ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Verify Kubernetes Files ==="
                    test -f k8s/deployment.yaml
                    test -f k8s/service.yaml

                    echo "=== Apply Deployment ==="
                    kubectl apply -f k8s/deployment.yaml

                    echo "=== Apply Service ==="
                    kubectl apply -f k8s/service.yaml

                    echo "=== Update Image ==="
                    kubectl set image \
                        deployment/${DEPLOYMENT} \
                        ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    echo "=== Wait for Rollout ==="
                    kubectl rollout status \
                        deployment/${DEPLOYMENT} \
                        --timeout=180s

                    echo "=== Verify Pods ==="
                    kubectl get pods -o wide

                    echo "=== Verify Services ==="
                    kubectl get services

                    echo "=== Deployment Health Check Passed ==="
                '''
            }
        }
    }

    post {

        success {
            echo 'SUCCESS: Food Ordering application deployed!'

            script {
                node('minikube-agent') {
                    sh '''
                        echo "=== CloudWatch: Pipeline Success ==="

                        if ! command -v aws >/dev/null 2>&1; then
                            echo "WARNING: AWS CLI is not installed."
                            exit 0
                        fi

                        aws cloudwatch put-metric-data \
                            --namespace "$CLOUDWATCH_NAMESPACE" \
                            --metric-name PipelineStatus \
                            --value 1 \
                            --unit Count \
                            --dimensions \
                                Application=FoodOrdering \
                                Pipeline=Jenkins \
                            --region "$AWS_REGION" \
                        || echo "WARNING: Failed to publish success metric."
                    '''
                }
            }
        }

        failure {
            echo 'FAILED: Check the stage that failed in Console Output.'

            script {
                node('minikube-agent') {
                    sh '''
                        echo "=== CloudWatch: Pipeline Failure ==="

                        if ! command -v aws >/dev/null 2>&1; then
                            echo "WARNING: AWS CLI is not installed."
                            exit 0
                        fi

                        aws cloudwatch put-metric-data \
                            --namespace "$CLOUDWATCH_NAMESPACE" \
                            --metric-name PipelineStatus \
                            --value 0 \
                            --unit Count \
                            --dimensions \
                                Application=FoodOrdering \
                                Pipeline=Jenkins \
                            --region "$AWS_REGION" \
                        || echo "WARNING: Failed to publish failure metric."
                    '''
                }
            }
        }

        always {
            echo 'CI/CD pipeline execution completed.'
            echo 'CloudWatch namespace: FoodOrdering/CI-CD'
        }
    }
}
