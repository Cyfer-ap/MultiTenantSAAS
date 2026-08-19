package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.chacha.multitenantsaas.config.NotificationEmailProperties;
import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.email.EmailDeliveryException;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDeliveryHandlerTest {

    @Mock private EmailSender emailSender;

    @Test
    void sendsEscapedHtmlWithAnAbsoluteApplicationLink() {
        NotificationEmailDeliveryHandler handler = handler("https://app.example.test/workspace/");
        NotificationDeliveryTask task =
                task(
                        "Task <assigned>\r\nnow",
                        "Open <script>alert('x')</script>\nReview it.",
                        "/projects/p-1?task=t-1&comment=c-1");

        handler.deliver(task);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        EmailMessage message = messageCaptor.getValue();

        assertThat(message.to()).isEqualTo("recipient@example.test");
        assertThat(message.subject()).isEqualTo("Task <assigned> now");
        assertThat(message.htmlContent())
                .contains("Task &lt;assigned&gt;")
                .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;<br>Review it.")
                .contains("href=\"https://app.example.test/projects/p-1?task=t-1&amp;comment=c-1\"")
                .doesNotContain("<script>");
    }

    @Test
    void omitsTheActionWhenNoTargetExists() {
        NotificationEmailDeliveryHandler handler = handler("http://localhost:8080");

        handler.deliver(task("Security alert", "Review account activity.", null));

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().htmlContent()).doesNotContain("View notification");
    }

    @Test
    void propagatesProviderFailureForTheOutboxRetryPolicy() {
        NotificationEmailDeliveryHandler handler = handler("https://app.example.test");
        NotificationDeliveryTask task = task("Task assigned", "Review it.", "/projects/p-1");
        EmailDeliveryException failure =
                new EmailDeliveryException(
                        "provider unavailable", new IllegalStateException("connection failed"));
        doThrow(failure).when(emailSender).send(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> handler.deliver(task)).isSameAs(failure);
    }

    @Test
    void rejectsUnsafeFrontendConfigurationAndTargets() {
        assertThatThrownBy(() -> handler("javascript:alert(1)"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Notification frontend base URL must be an HTTP(S) origin/base URL");

        NotificationEmailDeliveryHandler handler = handler("https://app.example.test");
        assertThatThrownBy(
                        () ->
                                handler.deliver(
                                        task(
                                                "Task assigned",
                                                "Review it.",
                                                "https://attacker.example/phishing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification email target URL must be an application-relative path");
    }

    private NotificationEmailDeliveryHandler handler(String frontendBaseUrl) {
        NotificationEmailProperties properties = new NotificationEmailProperties();
        properties.setFrontendBaseUrl(frontendBaseUrl);
        return new NotificationEmailDeliveryHandler(emailSender, properties);
    }

    private NotificationDeliveryTask task(String title, String body, String targetUrl) {
        return new NotificationDeliveryTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "recipient@example.test",
                NotificationDeliveryChannel.EMAIL,
                NotificationType.TASK_ASSIGNED,
                title,
                body,
                targetUrl);
    }
}
