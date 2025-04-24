package info.sup.proj.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "azure.openai")
@Data
public class AzureOpenAiConfig {
    private String endpoint;
    private String apiKey;
    private String deploymentName;
}