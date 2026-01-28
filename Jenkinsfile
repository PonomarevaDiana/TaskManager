pipeline {
    agent any

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_LOGIN = credentials('sonarqube-token')
        BUILD_NUMBER = "${BUILD_NUMBER}"
        GITVERSE_CREDENTIALS = credentials('gitverse-credentials')
        GIT_URL = 'https://gitverse.ru/stazhirovka2025/app'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/master']],
                    userRemoteConfigs: [[
                        url: "${GIT_URL}",
                        credentialsId: 'gitverse-credentials'
                    ]]
                ])
            }
        }

        stage('Build - Auth Service') {
            steps {
                sh '''
                    echo "Building Auth Service..."
                    cd authApp
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Build - Business Logic') {
            steps {
                sh '''
                    echo "Building Business Logic..."
                    cd businessLogic
                    mvn clean package -DskipTests
                '''
            }

        }

        stage('SonarQube Analysis - Auth Service') {
            steps {
                sh '''
                    echo "Analyzing Auth Service with SonarQube..."
                    cd authApp
                    mvn sonar:sonar \
                        -Dsonar.projectKey=auth-service \
                        -Dsonar.projectName="Auth Service" \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN} \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                '''
            }
        }

        stage('SonarQube Analysis - Business Logic') {
            steps {
                sh '''
                    echo "Analyzing Business Logic with SonarQube..."
                    cd businessLogic
                    mvn sonar:sonar \
                        -Dsonar.projectKey=business-logic \
                        -Dsonar.projectName="Business Logic" \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN} \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                '''
            }
        }

        stage('Quality Gate') {
            steps {
                sh '''
                    echo "Auth Service Quality Gate:"
                    curl -s -u "${SONAR_LOGIN}:" \
                        "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=auth-service" | grep -o '"status":"[^"]*"'

                    echo ""
                    echo "Business Logic Quality Gate:"
                    curl -s -u "${SONAR_LOGIN}:" \
                        "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=business-logic" | grep -o '"status":"[^"]*"'
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    echo "Building Docker images..."
                    docker build -t auth-service:${BUILD_NUMBER} ./authApp
                    docker build -t business-logic:${BUILD_NUMBER} ./businessLogic
                    docker build -t task-manager-frontend:${BUILD_NUMBER} ./taskManagerWEB
                '''
            }
        }

        stage('Stop Old Services') {
            steps {
                sh '''
                    echo "Stopping old containers..."
                    docker compose down || true
                '''
            }
        }

        stage('Start New Services') {
            steps {
                sh '''
                    echo "Starting new containers..."
                    docker compose up -d --build
                    sleep 20
                '''
            }
        }

        stage('Verify Services') {
            steps {
                sh '''
                    #!/bin/bash
                    echo "Verifying Services..."

                    for i in $(seq 1 30); do
                        if curl -s http://auth-service:8081/actuator/health | grep -q '"status":"UP"'; then
                            echo "Auth Service is UP"
                            break
                        fi
                        if [ $i -eq 30 ]; then
                            echo "Auth Service failed to start"
                            exit 1
                        fi
                        sleep 1
                    done

                    for i in $(seq 1 30); do
                        if curl -s http://business-logic:8080/actuator/health | grep -q '"status":"UP"'; then
                            echo "Business Logic is UP"
                            break
                        fi
                        if [ $i -eq 30 ]; then
                            echo "Business Logic failed to start"
                            exit 1
                        fi
                        sleep 1
                    done

                    FE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://frontend:80/)
                    if [ "$FE_STATUS" = "200" ]; then
                        echo "Frontend is UP"
                    fi
                '''
            }
        }

        stage('Report') {
            steps {
                sh '''
                    echo "=== DEPLOYMENT REPORT ===" > deployment_report.txt
                    echo "Build: #${BUILD_NUMBER}" >> deployment_report.txt
                    echo "Time: $(date)" >> deployment_report.txt
                    echo "Branch: ${GIT_BRANCH}" >> deployment_report.txt
                    echo "Commit: ${GIT_COMMIT_SHORT}" >> deployment_report.txt
                    echo "" >> deployment_report.txt

                    echo "SonarQube Analysis Results:" >> deployment_report.txt
                    curl -s -u "${SONAR_LOGIN}:" \
                        "${SONAR_HOST_URL}/api/measures/component?component=auth-service&metricKeys=bugs,vulnerabilities,code_smells,coverage" >> deployment_report.txt

                    echo "" >> deployment_report.txt
                    echo "Services Status:" >> deployment_report.txt
                    docker ps --format "table {{.Names}}\t{{.Status}}" >> deployment_report.txt
                    cat deployment_report.txt
                '''
            }
        }

        stage('Archive Results') {
            steps {
                sh '''
                    echo "Archiving results..."
                '''
                archiveArtifacts artifacts: '**/*.jar,deployment_report.txt', allowEmptyArchive: true
                cleanWs(deleteDirs: true, patterns: [[pattern: 'target/**', type: 'INCLUDE']])
            }
        }
    }
}