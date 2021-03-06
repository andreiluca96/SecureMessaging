package com.example.andrluc.securemessaging.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedList;
import com.example.andrluc.securemessaging.model.ConversationHistory;
import com.example.andrluc.securemessaging.model.MessageEntry;
import com.example.andrluc.securemessaging.model.MessageEntryDTO;
import com.example.andrluc.securemessaging.model.PublicKeyEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ConversationUtil {
    private static final int CONVERSATION_PORT = 12345;
    private static final ConversationHistory CONVERSATION_HISTORY;
    private static final String CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY = "ConversationHistory";

    private ConversationUtil() {}

    static {
        CONVERSATION_HISTORY = new ConversationHistory();
        CONVERSATION_HISTORY.setMessageEntries(new ArrayList<>());
    }

    public static void startConversationReceiver() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(CONVERSATION_PORT)) {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                        MessageEntryDTO messageEntryDTO = new ObjectMapper().readValue(in.readLine(), MessageEntryDTO.class);

                        RSAPublicKeySpec rsaPublicKeySpec = null;
                        PaginatedList<PublicKeyEntry> query = DynamoDBUtil.getDynamoDBMapper().scan(PublicKeyEntry.class, new DynamoDBScanExpression());
                        for (PublicKeyEntry publicKeyEntry : query) {
                            if (Objects.equals(publicKeyEntry.getUsername(), messageEntryDTO.getSender())) {
                                String[] split = publicKeyEntry.getPublicKey().split("\\|");
                                rsaPublicKeySpec = new RSAPublicKeySpec(new BigInteger(split[0]), new BigInteger(split[1]));
                            }
                        }

                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

                        IvParameterSpec iv = new IvParameterSpec(messageEntryDTO.getIv());
                        SecretKeySpec skeySpec = new SecretKeySpec(CryptoUtil.decrypt(publicKey, messageEntryDTO.getEncryptionKey()), "AES");

                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

                        String message = new String(cipher.doFinal(messageEntryDTO.getEncryptedMessage()));

                        MessageEntry messageEntry = new MessageEntry();
                        messageEntry.setReceiver(messageEntryDTO.getReceiver());
                        messageEntry.setSender(messageEntryDTO.getSender());
                        messageEntry.setDate(messageEntryDTO.getDate());
                        messageEntry.setMessage(message);

                        if (!CONVERSATION_HISTORY.getMessageEntries().contains(messageEntry)) {
                            CONVERSATION_HISTORY.getMessageEntries().add(messageEntry);
                        } else {
                            System.out.println("Avem deja");
                        }

                        System.out.println(messageEntry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static ConversationHistory getConversationHistory() {
        return CONVERSATION_HISTORY;
    }

    public static void startConversationWriter(final AppCompatActivity context) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            try {
                editor.putString(CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY, new ObjectMapper().writeValueAsString(CONVERSATION_HISTORY));
                editor.apply();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public static void loadConversationFromFile(AppCompatActivity context) {
        SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
        ConversationHistory conversationHistory = null;
        try {
            conversationHistory = new ObjectMapper().readValue(sharedPref.getString(CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY, null), ConversationHistory.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (conversationHistory != null) {
            if (conversationHistory.getMessageEntries() != null) {
                CONVERSATION_HISTORY.setMessageEntries(conversationHistory.getMessageEntries());
            }
        }

    }
}
