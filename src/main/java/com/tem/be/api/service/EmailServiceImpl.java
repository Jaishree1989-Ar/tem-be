package com.tem.be.api.service;

import com.tem.be.api.exception.EmailAuthenticationException;
import com.tem.be.api.exception.EmailServiceException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Log4j2
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.sender}")
    private String fromEmail;

    private static final String AUTH_FAILURE_MSG = "Email authentication failed. Please check MailerSend configuration.";
    private static final String SERVICE_FAILURE_MSG = "Failed to send email via MailerSend.";
    private static final String UNEXPECTED_FAILURE_MSG = "Failed to send email due to unexpected error.";

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        log.info("EmailServiceImpl.sendEmail() >> Entered for recipient: {}", to);
        if (isHtml) {
            sendHtmlEmail(to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully via MailerSend to: {}", to);
        } catch (Exception e) {
            handleEmailException(e, to, "email");
        }
        log.info("EmailServiceImpl.sendEmail() >> Exited");
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("EmailServiceImpl.sendHtmlEmail() >> Entered for recipient: {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("HTML Email sent successfully via MailerSend to: {}", to);
        } catch (Exception e) {
            handleEmailException(e, to, "HTML email");
        }
        log.info("EmailServiceImpl.sendHtmlEmail() >> Exited");
    }

    /**
     * Helper method to handle email sending exceptions, reducing code duplication.
     * Throws a custom, more specific exception for better error handling.
     */
    private void handleEmailException(Exception e, String recipient, String emailType) {
        if (e instanceof MailAuthenticationException) {
            log.error("MailerSend authentication failed when sending {}. Please check your SMTP credentials. Recipient: {}", emailType, recipient, e);
            throw new EmailAuthenticationException(AUTH_FAILURE_MSG, e);
        } else if (e instanceof MailException) {
            log.error("MailerSend service error when sending {}. Recipient: {}", emailType, recipient, e);
            throw new EmailServiceException(SERVICE_FAILURE_MSG, e);
        } else {
            log.error("Unexpected error sending {} to: {}.", emailType, recipient, e);
            throw new EmailServiceException(UNEXPECTED_FAILURE_MSG, e);
        }
    }

    /**
     * Test MailerSend connectivity
     */
    public boolean testConnection() {
        try {
            mailSender.createMimeMessage();
            log.info("MailerSend connection test successful");
            return true;
        } catch (Exception e) {
            log.error("MailerSend connection test failed", e);
            return false;
        }
    }
}