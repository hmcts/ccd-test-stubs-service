package uk.gov.hmcts.reform.ccd.test.stubs.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;

@Configuration
public class ApplicationConfig {

    @Bean(name = "httpClient")
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper;
    }
}
