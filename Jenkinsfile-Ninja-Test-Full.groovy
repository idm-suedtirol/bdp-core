pipeline {
    agent {
        dockerfile {
            filename 'docker/dockerfile-java'
            additionalBuildArgs '--build-arg JENKINS_USER_ID=`id -u jenkins` --build-arg JENKINS_GROUP_ID=`id -g jenkins`'
        }
    }

    environment {
        NINJA_ROOT_PATH = "ninja"
        NINJA_LOGGING_FILE = "/var/log/opendatahub/ninja.log"
        NINJA_SWAGGER_READMEURL = "https://github.com/noi-techpark/bdp-core/blob/development/ninja/README.md"
        NINJA_OAUTH_PUBKEY = credentials('bdp-core-ninja-pubkey')
        NINJA_OAUTH_ISSUER = "https://auth.testingmachine.eu"
        NINJA_OAUTH_SCOPES = "api1"
        NINJA_OAUTH_AUDIEN = "https://ipchannels.integreen-life.bz.it/ninja"

        BDP_DATABASE_SCHEMA = "intimev2"
        BDP_DATABASE_HOST = "test-pg-bdp.co90ybcr8iim.eu-west-1.rds.amazonaws.com"
        BDP_DATABASE_PORT = "5432"
        BDP_DATABASE_NAME = "bdp"
        BDP_DATABASE_READ_USER = "bdp_readonly"
        BDP_DATABASE_READ_PASSWORD = credentials('bdp-core-test-database-read-password')
    }

    stages {
        stage('Configure') {
            steps {
                sh '''
                    cp "${NINJA_ROOT_PATH}/src/main/resources/application.properties.dist" "${NINJA_ROOT_PATH}/src/main/resources/application.properties"
                    sed -i -e "s%\\(logging.file\\s*=\\).*\\$%\\1${NINJA_LOGGING_FILE}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(logging.level.root\\s*=\\).*\\$%\\1INFO%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(logging.level.org.springframework.jdbc.core\\s*=\\).*\\$%\\1WARN%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(ninja.swagger.readme-url\\s*=\\).*\\$%\\1 ${NINJA_SWAGGER_READMEURL}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(ninja.security.oauth2.pubkey\\s*=\\).*\\$%\\1 ${NINJA_OAUTH_PUBKEY}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(ninja.security.oauth2.issuer\\s*=\\).*\\$%\\1 ${NINJA_OAUTH_ISSUER}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(ninja.security.oauth2.scopes\\s*=\\).*\\$%\\1 ${NINJA_OAUTH_SCOPES}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties
                    sed -i -e "s%\\(ninja.security.oauth2.audien\\s*=\\).*\\$%\\1 ${NINJA_OAUTH_AUDIEN}%" ${NINJA_ROOT_PATH}/src/main/resources/application.properties

                    cp "${NINJA_ROOT_PATH}/src/main/resources/database.properties.dist" "${NINJA_ROOT_PATH}/src/main/resources/database.properties"
                    sed -i -e "s%\\(username\\s*=\\).*\\$%\\1 ${BDP_DATABASE_READ_USER}%" ${NINJA_ROOT_PATH}/src/main/resources/database.properties
                    sed -i -e "s%\\(password\\s*=\\).*\\$%\\1 ${BDP_DATABASE_READ_PASSWORD}%" ${NINJA_ROOT_PATH}/src/main/resources/database.properties
                    sed -i -e "s%\\(jdbcUrl\\s*=\\).*\\$%\\1 jdbc:postgresql://${BDP_DATABASE_HOST}:${BDP_DATABASE_PORT}/${BDP_DATABASE_NAME}?currentSchema=${BDP_DATABASE_SCHEMA},public%" ${NINJA_ROOT_PATH}/src/main/resources/database.properties
                '''
            }
        }
        stage('Build - Ninja') {
            steps {
                sh 'cd ${NINJA_ROOT_PATH} && mvn -B -U clean test package'
            }
        }
        stage('Archive') {
            steps {
                sh 'cp ${NINJA_ROOT_PATH}/target/v2.war v2.war'
                archiveArtifacts artifacts: 'v2.war', onlyIfSuccessful: true
            }
        }
    }
}