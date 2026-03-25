package io.github.dushyna.ticketflow.configuration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "digitalocean.spaces")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsS3Configuration {
    String accessKey;
    String secretKey;
    String endpoint;
    String region;
    String bucketName;
}
