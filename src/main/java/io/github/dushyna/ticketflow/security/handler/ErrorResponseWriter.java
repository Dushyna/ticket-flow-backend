package io.github.dushyna.ticketflow.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.security.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public class ErrorResponseWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void write(HttpServletResponse response, ErrorResponse errorResponse) throws IOException {
        response.setStatus(errorResponse.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getWriter(), errorResponse);
    }
}
