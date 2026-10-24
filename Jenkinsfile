```groovy
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

                echo '=== Latest Git Commit ==='
                sh '''
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

                    echo "=== Check Packaged Application Resources ==="
                    jar tf target/food-ordering-0.0.1-SNAPSHOT.jar \
                      | grep -E 'BOOT-INF/classes/(static|templates)/.*index.html' \
                      || true
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

                    echo "=== Agent ==="
                    hostname
                    whoami

                    echo "=== Minikube Configuration ==="
                    test -f /home/ec2-user/.minikube/profiles/minikube/config.json
                    test -f /home/ec2-user/.kube/config

                    echo "=== Minikube Status ==="
                    /usr/local/bin/minikube status -p minikube

                    echo "=== Kubernetes Context ==="
                    kubectl config current-context
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

                    echo "=== Verify Built JAR ==="
                    test -f target/food-ordering-0.0.1-SNAPSHOT.jar

                    echo "=== Build Unique Image Without Cache ==="
                    /usr/local/bin/minikube image build \
                        --no-cache \
                        -p "${MINIKUBE_PROFILE}" \
                        -t "${IMAGE_NAME}:${BUILD_NUMBER}" .

                    echo "=== Verify Image Exists ==="
                    /usr/local/bin/minikube image ls -p "${MINIKUBE_PROFILE}" \
                        | grep -F "${IMAGE_NAME}:${BUILD_NUMBER}"
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

                    echo "=== Verify Kubernetes Connection ==="
                    kubectl config use-context minikube
                    kubectl get nodes

                    echo "=== Verify Manifest Files ==="
                    test -f k8s/deployment.yaml
                    test -f k8s/service.yaml

                    echo "=== Apply Kubernetes Manifests ==="
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    echo "=== Update Deployment Image ==="
                    kubectl set image \
                        deployment/${DEPLOYMENT} \
                        ${CONTAINER}=${IMAGE_NAME}:${BUILD_NUMBER}

                    echo "=== Wait for Rollout ==="
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
                    kubectl get service food-ordering-service

                    echo "=== Verify Running Container Image ==="
                    kubectl get pods \
                        -l app=food-ordering \
                        -o jsonpath='{range .items[*]}{.metadata.name}{" : "}{.spec.containers[0].image}{"\\n"}{end}'

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
                    withEnv([
                        "METRIC_STATUS=1"
                    ]) {
                        sh '''
                            echo "=== CloudWatch Success Metric ==="

                            if command -v aws >/dev/null 2>&1; then
                                aws cloudwatch put-metric-data \
                                    --namespace "${CLOUDWATCH_NAMESPACE}" \
                                    --metric-name PipelineStatus \
                                    --value "${METRIC_STATUS}" \
                                    --unit Count \
                                    --region "${AWS_REGION}" \
                                || echo "WARNING: CloudWatch metric failed."
                            else
                                echo "WARNING: AWS CLI is not installed."
                            fi
                        '''
                    }
                }
            }
        }

        failure {
            echo 'FAILED: Check the failed stage in Console Output.'

            script {
                node('minikube-agent') {
                    withEnv([
                        "METRIC_STATUS=0"
                    ]) {
                        sh '''
                            echo "=== CloudWatch Failure Metric ==="

                            if command -v aws >/dev/null 2>&1; then
                                aws cloudwatch put-metric-data \
                                    --namespace "${CLOUDWATCH_NAMESPACE}" \
                                    --metric-name PipelineStatus \
                                    --value "${METRIC_STATUS}" \
                                    --unit Count \
                                    --region "${AWS_REGION}" \
                                || echo "WARNING: CloudWatch metric failed."
                            else
                                echo "WARNING: AWS CLI is not installed."
                            fi
                        '''
                    }
                }
            }
        }

        always {
            echo 'CI/CD pipeline execution completed.'
            echo 'CloudWatch namespace: FoodOrdering/CI-CD'
        }
    }
}
```
