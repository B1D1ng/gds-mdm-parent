package com.ebay.behavior.gds.mdm.commonSvc.service;

import jakarta.mail.MessagingException;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.DEFAULT_RETRY_BACKOFF;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.SMALL_RETRY_MAX_ATTEMPTS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Service
public class EmailService {

    @Autowired
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Retryable(retryFor = MessagingException.class, maxAttempts = SMALL_RETRY_MAX_ATTEMPTS, backoff = @Backoff(delay = DEFAULT_RETRY_BACKOFF))
    public void sendSimpleMessage(String[] to, String subject, String text) {
        val message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        emailSender.send(message);
    }

    @Retryable(retryFor = MessagingException.class, maxAttempts = SMALL_RETRY_MAX_ATTEMPTS, backoff = @Backoff(delay = DEFAULT_RETRY_BACKOFF))
    public void sendHtmlMessage(String[] to, String subject, String htmlBody) throws MessagingException {
        val message = emailSender.createMimeMessage();
        val helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        emailSender.send(message);
    }
}