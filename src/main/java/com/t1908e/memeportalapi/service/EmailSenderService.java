package com.t1908e.memeportalapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender javaMailSender;

    public void sendSimpleEmail(String toEmail, String body, String subject) throws Exception {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setText(body);
            simpleMailMessage.setTo(toEmail);
            simpleMailMessage.setSubject(subject);
            javaMailSender.send(simpleMailMessage);
        } catch (Exception exception) {
            throw new Exception("Send mail to " + toEmail + "failed: " + exception.getMessage());
        }
    }
}
