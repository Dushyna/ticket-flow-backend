package io.github.dushyna.ticketflow.mail.exception;

public class TemplateProcessingException extends EmailException {
    public TemplateProcessingException(String templateName, Throwable cause) {
        super("Failed to process template: " + templateName, cause);
    }
}

