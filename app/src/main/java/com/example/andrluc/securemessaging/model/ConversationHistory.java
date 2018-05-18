package com.example.andrluc.securemessaging.model;

import java.util.List;

public class ConversationHistory {
    private List<MessageEntry> messageEntries;

    public List<MessageEntry> getMessageEntries() {
        return messageEntries;
    }

    public void setMessageEntries(List<MessageEntry> messageEntries) {
        this.messageEntries = messageEntries;
    }
}
