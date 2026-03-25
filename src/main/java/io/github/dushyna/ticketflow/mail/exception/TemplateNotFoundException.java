package io.github.dushyna.ticketflow.mail.exception;

public class TemplateNotFoundException extends EmailException {
    public TemplateNotFoundException(String templateName) {
        super("Template not found: " + templateName);
    }
}

