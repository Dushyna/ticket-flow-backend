package io.github.dushyna.ticketflow.mail.exception;

public class EmailSendingException extends EmailException {
    public EmailSendingException(String recipientEmail, Throwable cause) {
        super("Failed to send email to " + recipientEmail, cause);
    }
}

