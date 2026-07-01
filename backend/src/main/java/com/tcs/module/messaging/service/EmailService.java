package com.tcs.module.messaging.service;

public interface EmailService {

    void sendPlainText(String toEmail, String subject, String body);
}
