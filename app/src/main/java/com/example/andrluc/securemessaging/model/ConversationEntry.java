package com.example.andrluc.securemessaging.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.Date;

@DynamoDBTable(tableName = "SecureMessaging-PublicKeysTable")
public class ConversationEntry {
    @DynamoDBHashKey(attributeName = "sender")
    private String sender;
    @DynamoDBRangeKey(attributeName = "receiver")
    private String receiver;
    @DynamoDBAttribute(attributeName = "senderEncrypted")
    private String senderEncrypted;
    @DynamoDBAttribute(attributeName = "receiverEncrypted")
    private String receiverEncrypted;
    @DynamoDBAttribute(attributeName = "timestamp")
    private Date timestamp;

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

    public String getSenderEncrypted() {
        return senderEncrypted;
    }

    public void setSenderEncrypted(String senderEncrypted) {
        this.senderEncrypted = senderEncrypted;
    }

    public String getReceiverEncrypted() {
        return receiverEncrypted;
    }

    public void setReceiverEncrypted(String receiverEncrypted) {
        this.receiverEncrypted = receiverEncrypted;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
