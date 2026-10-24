
pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

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
                deleteDir()
                checkout scm

                sh '''
                    set -eu
                    echo "=== Latest Git Commit ==="
                    git log -1 --oneline
                    echo "Build number: ${BUILD_NUMBER}"
                '''

                stash name: 'source-code',
                      includes: '**/*',
                      excludes: '.git/**'
            }
        }

        stage('Build and Test') {
            agent { label 'built-in' }

            steps {
                deleteDir()
                unstash 'source-code'

                sh '''
                    set -eu

                    echo "=== Java Version ==="
                    java -version

                    echo "=== Maven Version ==="
                    mvn -version

                    echo "=== Build and Test ==="
                    mvn clean test package

                    echo "=== Verify JAR ==="
                    test -f target/food-ordering-0.0.1-SNAPSHOT.jar
                    ls -lh target/food-ordering-0.0.1-SNAPSHOT.jar

                    echo "=== Verify HTML Resource ==="
                    jar tf target/food-ordering-0.0.1-SNAPSHOT.jar \
                        | grep 'BOOT-INF/classes/static/index.html'
                '''

                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true
                )

                stash name: 'built-application',
                      includes: 'target/*.jar'
            }
        }

        stage('Check Minikube Agent') {
            agent { label 'minikube-agent' }

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                sh '''
                    set -eu

                    echo "=== Agent Information ==="
                    hostname
                    whoami

                    echo "=== Verify Minikube Configuration ==="
                    test -f /home/ec2-user/.minikube/profiles/minikube/config.json
                    test -f /home/ec2-user/.kube/config

                    echo "=== Minikube Status ==="
                    /usr/local/bin/minikube status -p minikube

                    echo "=== Kubernetes Context ==="
                    kubectl config use-context minikube
                    kubectl get nodes
                '''
            }
        }

        stage('Build Fresh Image') {
            agent { label 'minikube-agent' }

            environment {
                HOME = '/home/ec2-user'
                MINIKUBE_HOME = '/home/ec2-user'
                KUBECONFIG = '/home/ec2-user/.kube/config'
                PATH = '/usr/local/bin:/usr/bin:/bin'
            }

            steps {
                deleteDir()
                unstash 'source-code'
                unstash 'built-application'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Verify JAR ==="
                    test -f target/food-ordering-0.0.1-SNAPSHOT.jar

                    echo "=== Verify Dockerfile ==="
                    test -f Dockerfile
                    cat Dockerfile

                    echo "=== Build Unique Minikube Image ==="
                    /usr/local/bin/minikube image build \
                        -p "${MINIKUBE_PROFILE}" \
                        -t "${IMAGE_NAME}:${BUILD_NUMBER}" .

                    echo "=== Verify Image ==="
                    /usr/local/bin/minikube image ls -p "${MINIKUBE_PROFILE}" \
                        | grep -F "${IMAGE_NAME}:${BUILD_NUMBER}"

                    echo "=== Image Build Successful ==="
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
                deleteDir()
                unstash 'source-code'

                sh '''
                    set -eu

                    export HOME=/home/ec2-user
                    export MINIKUBE_HOME=/home/ec2-user
                    export KUBECONFIG=/home/ec2-user/.kube/config
                    export PATH=/usr/local/bin:/usr/bin:/bin

                    echo "=== Connect to Minikube ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Verify Kubernetes Manifests ==="
                    test -f k8s/deployment.yaml
                    test -f k8s/service.yaml

                    echo "=== Apply Deployment and Service ==="
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    echo "=== Update Application Image ==="
                    kubectl set image \
                        deployment/${DEPLOYMENT} \
                        ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    echo "=== Wait for Deployment Rollout ==="
                    kubectl rollout status \
                        deployment/${DEPLOYMENT} \
                        --timeout=180s

                    echo "=== Verify Deployment Image ==="
                    kubectl get deployment "${DEPLOYMENT}" \
                        -o jsonpath='{.spec.template.spec.containers[?(@.name=="food-ordering")].image}'
                    echo

                    echo "=== Verify Pods ==="
                    kubectl get pods -o wide

                    echo "=== Verify Service ==="
                    kubectl get services

                    echo "=== Verify Rollout History ==="
                    kubectl rollout history deployment/${DEPLOYMENT}

                    echo "=== Deployment Successful ==="
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
                        echo "=== CloudWatch Success Metric ==="

                        if command -v aws >/dev/null 2>&1; then
                            aws cloudwatch put-metric-data \
                                --namespace "${CLOUDWATCH_NAMESPACE}" \
                                --metric-name PipelineStatus \
                                --value 1 \
                                --unit Count \
                                --region "${AWS_REGION}" \
                            || echo "WARNING: Could not publish success metric."
                        else
                            echo "WARNING: AWS CLI is not installed."
                        fi
                    '''
                }
            }
        }

        failure {
            echo 'FAILED: Check the failed stage in Console Output.'

            script {
                node('minikube-agent') {
                    sh '''
                        echo "=== CloudWatch Failure Metric ==="

                        if command -v aws >/dev/null 2>&1; then
                            aws cloudwatch put-metric-data \
                                --namespace "${CLOUDWATCH_NAMESPACE}" \
                                --metric-name PipelineStatus \
                                --value 0 \
                                --unit Count \
                                --region "${AWS_REGION}" \
                            || echo "WARNING: Could not publish failure metric."
                        else
                            echo "WARNING: AWS CLI is not installed."
                        fi
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

