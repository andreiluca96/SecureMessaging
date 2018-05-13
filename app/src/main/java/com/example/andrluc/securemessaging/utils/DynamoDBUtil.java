package com.example.andrluc.securemessaging.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.andrluc.securemessaging.BuildConfig;

public class DynamoDBUtil {
    private static DynamoDBMapper dynamoDBMapper;

    private DynamoDBUtil() {

    }

    static {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(BuildConfig.ACCESS_KEY, BuildConfig.SECRET_KEY);
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCreds);
        dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();

    }

    public static DynamoDBMapper getDynamoDBMapper() {
        return dynamoDBMapper;
    }
}
