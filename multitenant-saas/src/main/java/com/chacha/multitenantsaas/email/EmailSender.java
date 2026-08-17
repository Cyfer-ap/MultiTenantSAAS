package com.chacha.multitenantsaas.email;

public interface EmailSender {

    void send(EmailMessage message);
}
