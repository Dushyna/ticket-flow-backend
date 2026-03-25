package io.github.dushyna.ticketflow.configuration;

import freemarker.template.Configuration;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class FreemarkerConfig {

    @Bean
    public Configuration freemarkerConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(),
                "mail_templates"
        );
        return configuration;
    }
}
