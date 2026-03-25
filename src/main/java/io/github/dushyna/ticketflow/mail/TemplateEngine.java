package io.github.dushyna.ticketflow.mail;

import io.github.dushyna.ticketflow.mail.exception.TemplateNotFoundException;
import io.github.dushyna.ticketflow.mail.exception.TemplateProcessingException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class TemplateEngine {

    private final Configuration freemarkerConfig;

    public TemplateEngine(@Qualifier("freemarkerConfiguration") Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    public String generateHtml(String templateName, Map<String, Object> model) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException e) {
            throw new TemplateNotFoundException(templateName);
        } catch (Exception e) {
            throw new TemplateProcessingException(templateName, e);
        }
    }

    public String generateHtml(String templateName) {
        return generateHtml(templateName, new HashMap<>());
    }
}
