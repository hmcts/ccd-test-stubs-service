FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

ENV APPLICATION_TOTAL_MEMORY 128M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 41
ENV JAVA_OPTS ""

COPY build/libs/ccd-test-stubs-service.jar /opt/app/
COPY wiremock/mappings /opt/app/wiremock/mappings

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:5555/health || exit 1

EXPOSE 5555

CMD [ "ccd-test-stubs-service.jar" ]
