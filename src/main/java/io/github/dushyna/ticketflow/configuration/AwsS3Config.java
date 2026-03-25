package io.github.dushyna.ticketflow.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * Configuration class for creating AWS S3 and DigitalOcean Spaces clients.
 * <p>
 * Loads properties prefixed with {@code digitalocean.spaces} into {@link AwsS3Configuration}
 * and uses them to build both synchronous {@link S3Client} and asynchronous
 * {@link S3AsyncClient} beans.
 * </p>
 *
 * <h3>Configuration Properties (application.yml or application.properties):</h3>
 * <ul>
 *   <li>{@code digitalocean.spaces.accessKey} - Access key for authentication.</li>
 *   <li>{@code digitalocean.spaces.secretKey} - Secret key for authentication.</li>
 *   <li>{@code digitalocean.spaces.endpoint} - Custom S3 endpoint (e.g., Spaces URL).</li>
 *   <li>{@code digitalocean.spaces.region} - AWS region or Space region (e.g., {@code "nyc3"}).</li>
 *   <li>{@code digitalocean.spaces.bucketName} - Default bucket (space) name.</li>
 * </ul>
 *
 * <p>Both clients are configured with:</p>
 * <ol>
 *   <li>Endpoint override to support non-AWS S3-compatible services.</li>
 *   <li>Static credentials provider using basic credentials.</li>
 *   <li>Region configuration.</li>
 *   <li>Path-style access enabled via {@link S3Configuration}.</li>
 * </ol>
 *
 * <p><strong>Example AwsS3Configuration:</strong></p>
 * <pre>{@code
 * @Configuration
 * @ConfigurationProperties(prefix = "digitalocean.spaces")
 * public class AwsS3Configuration {
 *     private String accessKey;
 *     private String secretKey;
 *     private String endpoint;
 *     private String region;
 *     private String bucketName;
 *     // getters/setters
 * }
 * }</pre>
 *
 * @see AwsS3Configuration
 * @see S3Client
 * @see S3AsyncClient
 */
@Configuration
@RequiredArgsConstructor
public class AwsS3Config {

    private final AwsS3Configuration awsConfig;

    /**
     * Creates and configures a synchronous {@link S3Client} for interacting with
     * AWS S3 or compatible services like DigitalOcean Spaces.
     *
     * @return configured {@link S3Client} bean
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(awsConfig.getEndpoint()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        awsConfig.getAccessKey(),
                                        awsConfig.getSecretKey()
                                )
                        )
                )
                .region(Region.of(awsConfig.getRegion()))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }

    /**
     * Creates and configures an asynchronous {@link S3AsyncClient} for interacting
     * with AWS S3 or compatible services like DigitalOcean Spaces.
     * <p>
     * Use this client for non-blocking file operations, e.g., via {@code @Async} methods.
     * </p>
     *
     * @return configured {@link S3AsyncClient} bean
     */
    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(awsConfig.getEndpoint()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        awsConfig.getAccessKey(),
                                        awsConfig.getSecretKey()
                                )
                        )
                )
                .region(Region.of(awsConfig.getRegion()))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }

    /**
     * ExecutorService for async S3 stream uploads.
     * Provides a fixed thread pool to offload blocking InputStream reads
     * and avoid blocking the SDK event-loop threads.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService s3ExecutorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("s3Executor-");
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }
}
