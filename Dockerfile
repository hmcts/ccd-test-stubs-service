ARG PLATFORM=""

FROM hmctspublic.azurecr.io/base/java${PLATFORM}:17-distroless
USER hmcts

ENV APPLICATION_TOTAL_MEMORY 128M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 41
ENV JAVA_OPTS ""

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/ccd-test-stubs-service.jar /opt/app/
COPY wiremock/mappings /opt/app/wiremock/mappings

EXPOSE 5555

CMD [ "ccd-test-stubs-service.jar" ]
