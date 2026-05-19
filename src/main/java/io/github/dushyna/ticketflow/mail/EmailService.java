package io.github.dushyna.ticketflow.mail;

import io.github.dushyna.ticketflow.booking.entity.Order;
import io.github.dushyna.ticketflow.booking.repository.OrderRepository;
import io.github.dushyna.ticketflow.booking.service.impl.TicketDocumentServiceImpl;
import io.github.dushyna.ticketflow.common.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Service for email sending
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final TranslationService translationService;
    private final TicketDocumentServiceImpl ticketDocumentService;
    private final OrderRepository orderRepository;

    @Async
    public void sendConfirmationEmail(String sentTo, String confirmationCode, Locale locale) {
        LocaleContextHolder.setLocale(locale);
        String confirmationLink = "%s/api/v1/auth/confirm-redirect/%s".formatted(baseUrl, confirmationCode);

        Map<String, Object> model = new HashMap<>();
        model.put("link", confirmationLink);

        model.put("t_title", translationService.get("mail.confirm.title"));
        model.put("t_header", translationService.get("mail.confirm.header"));
        model.put("t_greeting", translationService.get("mail.confirm.greeting"));
        model.put("t_thanks", translationService.get("mail.confirm.thanks"));
        model.put("t_instruction", translationService.get("mail.confirm.instruction"));
        model.put("t_button", translationService.get("mail.confirm.button"));
        model.put("t_fallback", translationService.get("mail.confirm.fallback"));
        model.put("t_footer_rights", translationService.get("mail.footer.rights"));

        String htmlContent = templateEngine.generateHtml("confirm_registration_mail.ftlh", model);
        String subject = translationService.get("mail.confirm.header");
        emailSender.sendEmail(sentTo, subject, htmlContent);
    }

    @Async
    public void sendResetPasswordEmail(String sentTo, String token, Locale locale) {
        LocaleContextHolder.setLocale(locale);

        String resetLink = "%s/reset-password?token=%s".formatted(frontendUrl, token);

        Map<String, Object> model = new HashMap<>();
        model.put("link", resetLink);

        model.put("t_title", translationService.get("mail.reset.title"));
        model.put("t_header", translationService.get("mail.reset.header"));
        model.put("t_greeting", translationService.get("mail.confirm.greeting"));
        model.put("t_text", translationService.get("mail.reset.text"));
        model.put("t_button", translationService.get("mail.reset.button"));
        model.put("t_ignore", translationService.get("mail.reset.ignore"));
        model.put("t_footer_rights", translationService.get("mail.footer.rights"));

        String htmlContent = templateEngine.generateHtml("reset_password_mail.ftlh", model);

        String subject = translationService.get("mail.reset.title");

        emailSender.sendEmail(sentTo, subject, htmlContent);
    }

    @Async
    @Transactional(readOnly = true)
    public void sendBookingConfirmationEmail(UUID orderId) {
        log.info("EMAIL_SERVICE: Async thread started for order UUID: {}", orderId);
        try {
            // FIXED: Fetch the fresh order with all matching collection sub-nodes instantly inside the thread session loop
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order layout tracking context not found for ID: " + orderId));

            var firstBooking = order.getBookings().getFirst();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

            String formattedShowtime = formatter.format(firstBooking.getShowtime().getStartTime());

            Map<String, Object> model = new HashMap<>();
            model.put("showtime", formattedShowtime);
            model.put("userName", order.getUser().getEmail());
            model.put("movieTitle", firstBooking.getShowtime().getMovie().getTitle());
            model.put("hallName", firstBooking.getHall().getName());
            model.put("totalPrice", order.getTotalPrice());
            model.put("ticketsCount", order.getBookings().size());
            model.put("orderId", order.getId().toString());

            model.put("t_thank_you", translationService.get("mail.thank_you"));
            model.put("t_order_info", translationService.get("mail.order_info"));
            model.put("t_hall", translationService.get("mail.hall"));
            model.put("t_time", translationService.get("mail.time"));
            model.put("t_tickets_count", translationService.get("mail.tickets_count"));
            model.put("t_total", translationService.get("mail.total"));
            model.put("t_title", translationService.get("mail.title"));

            String htmlContent = templateEngine.generateHtml("booking_confirmation_mail.ftlh", model);
            String subject = translationService.get("payment.success.ticket_subject");

            byte[] orderTicketsPdfBytes = ticketDocumentService.generateOrderTicketsPdf(order.getId());

            String fileName = String.format("Tickets_%s_Order_%s.pdf",
                    firstBooking.getShowtime().getMovie().getTitle().replaceAll("\\s+", "_"),
                    order.getId().toString().substring(0, 8)
            );

            Map<String, byte[]> attachments = new HashMap<>();
            attachments.put(fileName, orderTicketsPdfBytes);

            log.info("EMAIL_SERVICE: Handing over email to EmailSender for user {}", order.getUser().getEmail());

            emailSender.sendEmailWithAttachments(order.getUser().getEmail(), subject, htmlContent, attachments);

            log.info("EMAIL_SERVICE: Email sent successfully!");
        } catch (Exception e) {
            log.error("EMAIL_SERVICE ERROR: Detailed error for order {}: ", orderId, e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void sendRefundNotificationEmail(Order order) {
        log.info("EMAIL_SERVICE: Sending refund notification for order: {}", order.getId());
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("userName", order.getUser().getEmail());
            model.put("totalPrice", order.getTotalPrice());
            model.put("orderId", order.getId().toString());

            // Add translations for the refund template
            model.put("t_subject", translationService.get("mail.refund.subject"));
            model.put("t_header", translationService.get("mail.refund.header"));
            model.put("t_message", translationService.get("mail.refund.message"));
            model.put("t_total", translationService.get("mail.total"));

            // Generate HTML using a new template: refund_notification_mail.ftlh
            String htmlContent = templateEngine.generateHtml("refund_notification_mail.ftlh", model);
            String subject = translationService.get("mail.refund.subject");

            emailSender.sendEmail(order.getUser().getEmail(), subject, htmlContent);
            log.info("EMAIL_SERVICE: Refund email sent successfully to {}", order.getUser().getEmail());
        } catch (Exception e) {
            log.error("EMAIL_SERVICE ERROR: Failed to send refund email for order {}: ", order.getId(), e);
        }
    }

}


