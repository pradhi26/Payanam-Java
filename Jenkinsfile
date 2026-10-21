
pipeline {
    agent any

    tools {
        maven 'M3'
    }

    stages {
        stage('Build and Test') {
            steps {
                bat 'mvn clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t payanam-app:latest .'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(
                    credentialsId: 'kubeconfig-cred',
                    variable: 'KUBECONFIG_FILE'
                )]) {
                    bat 'set KUBECONFIG=%KUBECONFIG_FILE% && kubectl apply -f k8s/deployment.yaml'
                    bat 'set KUBECONFIG=%KUBECONFIG_FILE% && kubectl apply -f k8s/service.yaml'
                    bat 'set KUBECONFIG=%KUBECONFIG_FILE% && kubectl rollout status deployment/payanam-deployment --timeout=120s'
                }
            }
        }
    }

    post {
        success {
            echo 'Payanam deployed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the console output.'
        }
    }
}
