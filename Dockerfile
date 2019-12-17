FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

ENV APPLICATION_TOTAL_MEMORY 128M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 41
ENV JAVA_OPTS ""

COPY lib/applicationinsights-agent-2.4.0-BETA-SNAPSHOT.jar lib/AI-Agent.xml /opt/app/
COPY build/libs/ccd-test-stubs-service.jar /opt/app/
COPY wiremock/mappings /opt/app/wiremock/mappings

EXPOSE 5555

CMD [ "ccd-test-stubs-service.jar" ]
