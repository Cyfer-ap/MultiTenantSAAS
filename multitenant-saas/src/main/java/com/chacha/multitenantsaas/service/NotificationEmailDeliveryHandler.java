package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.NotificationEmailProperties;
import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NotificationEmailDeliveryHandler implements NotificationDeliveryHandler {

    private final EmailSender emailSender;
    private final URI frontendBaseUri;

    public NotificationEmailDeliveryHandler(
            EmailSender emailSender, NotificationEmailProperties properties) {
        this.emailSender = emailSender;
        this.frontendBaseUri = validateFrontendBaseUrl(properties.getFrontendBaseUrl());
    }

    @Override
    public NotificationDeliveryChannel channel() {
        return NotificationDeliveryChannel.EMAIL;
    }

    @Override
    public void deliver(NotificationDeliveryTask task) {
        emailSender.send(
                new EmailMessage(
                        task.recipientEmail(), normalizeSubject(task.title()), buildHtml(task)));
    }

    private String buildHtml(NotificationDeliveryTask task) {
        String title = escape(task.title());
        String body = escape(task.body()).replace("\n", "<br>");
        String action = buildAction(task.targetUrl());

        return """
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#1f2937">
                  <h2 style="margin-bottom:8px">%s</h2>
                  <p>%s</p>
                  %s
                  <p style="color:#6b7280;font-size:13px">
                    This notification was sent by MultiTenant SaaS.
                  </p>
                </div>
                """
                .formatted(title, body, action);
    }

    private String buildAction(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return "";
        }
        if (!targetUrl.startsWith("/") || targetUrl.startsWith("//")) {
            throw new IllegalArgumentException(
                    "Notification email target URL must be an application-relative path");
        }

        String absoluteTarget = frontendBaseUri.resolve(targetUrl).toASCIIString();
        return """
                <p style="margin:24px 0">
                  <a href="%s"
                     style="display:inline-block;padding:12px 18px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px">
                    View notification
                  </a>
                </p>
                """
                .formatted(escape(absoluteTarget));
    }

    private String normalizeSubject(String subject) {
        return subject.replaceAll("[\\r\\n]+", " ").trim();
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private URI validateFrontendBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalStateException("Notification frontend base URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(rawBaseUrl.trim().replaceAll("/+$", "") + "/");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Notification frontend base URL must be a valid HTTP(S) URL", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "Notification frontend base URL must be an HTTP(S) origin/base URL");
        }
        return uri;
    }
}
