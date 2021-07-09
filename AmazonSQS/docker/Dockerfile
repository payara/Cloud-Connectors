FROM payara/micro:5.2021.1-jdk11
MAINTAINER CSC Team Enigma <enigma_development@cscglobal.com>

USER root
RUN apk --no-cache add curl

#COPY logging.properties /config/logging.properties
COPY tmp/ $DEPLOY_DIR
EXPOSE 8080

CMD ["--deploymentDir", "/opt/payara/deployments", "--port", "8080"]
