package io.github.dushyna.ticketflow.base;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@Import(BaseIT.RestTemplateConfig.class)
public abstract class BaseIT {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    public static class RestTemplateConfig {
        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

            return new RestTemplateBuilder().additionalMessageConverters(
                    new MappingJackson2HttpMessageConverter(mapper));
        }
    }

    protected HttpHeaders getHeadersWithCsrf() {
        ResponseEntity<Void> response = restTemplate.getForEntity("/api/v1/auth/me", Void.class);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String csrfToken = "";

        if (setCookie != null && setCookie.contains("XSRF-TOKEN")) {
            String[] parts = setCookie.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("XSRF-TOKEN=")) {
                    csrfToken = part.split("=")[1];
                    break;
                }
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!csrfToken.isEmpty()) {
            headers.add("X-XSRF-TOKEN", csrfToken);
            headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + csrfToken);
        }

        return headers;
    }

}
