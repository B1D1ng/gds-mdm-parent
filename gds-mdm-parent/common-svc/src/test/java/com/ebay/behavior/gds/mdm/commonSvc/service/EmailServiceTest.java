package com.ebay.behavior.gds.mdm.commonSvc.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith({MockitoExtension.class})
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private final String from = "DL-eBay-trk-onbrdg-tool-dev@ebay.com";

    @Test
    void sendSimpleMessage() {
        setField(emailService, "from", from);
        var to = new String[]{"to@example.com"};
        var subject = "Test SendSimpleMessage";
        var body = "Test Body";
        emailService.sendSimpleMessage(to, subject, body);

        var messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        var sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getFrom()).isEqualTo(from);
        assertThat(sentMessage.getTo()).isEqualTo(to);
        assertThat(sentMessage.getSubject()).isEqualTo(subject);
        assertThat(sentMessage.getText()).isEqualTo(body);
    }

    @Test
    void sendHtmlMessage() throws MessagingException {
        setField(emailService, "from", from);
        var to = new String[]{"to@example.com"};
        var subject = "Test SendHtmlMessage";
        var body = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "    <h1>Hello, World!</h1>\n" +
                "</body>\n" +
                "</html>";

        var mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService.sendHtmlMessage(to, subject, body);

        var messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
    }
}