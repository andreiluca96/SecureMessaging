package com.example.andrluc.securemessaging.model;

import java.util.Date;

import javax.crypto.SealedObject;

public class MessageEntryDTO {
    private SealedObject encryptedMessage;
    private String sender;
    private String receiver;
    private Date date;

    public SealedObject getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(SealedObject encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
