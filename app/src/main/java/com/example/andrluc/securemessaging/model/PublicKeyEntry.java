package com.example.andrluc.securemessaging.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

/**
 * Created by Luca Andrei on 5/13/2018.
 */

@DynamoDBTable(tableName = "SecureMessaging-PublicKeysTable")
public class PublicKeyEntry {
    @DynamoDBHashKey(attributeName = "publicKey")
    private String publicKey;

    @DynamoDBAttribute(attributeName = "macAddress")
    private String macAddress;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
