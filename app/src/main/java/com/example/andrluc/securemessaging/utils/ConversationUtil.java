package com.example.andrluc.securemessaging.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import com.example.andrluc.securemessaging.model.ConversationHistory;
import com.example.andrluc.securemessaging.model.MessageEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConversationUtil {
    private static final int CONVERSATION_PORT = 12345;
    private static final ConversationHistory CONVERSATION_HISTORY;
    private static final String CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY = "ConversationHistory";

    private ConversationUtil() {}

    static {
        CONVERSATION_HISTORY = new ConversationHistory();
        CONVERSATION_HISTORY.setMessageEntries(new ArrayList<MessageEntry>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(CONVERSATION_PORT)) {
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                        MessageEntry messageEntry = new ObjectMapper().readValue(in.readLine(), MessageEntry.class);

                        CONVERSATION_HISTORY.getMessageEntries().add(messageEntry);

                        System.out.println(messageEntry);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static ConversationHistory getConversationHistory() {
        return CONVERSATION_HISTORY;
    }

    public static void startConversationWriter(final AppCompatActivity context) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                try {
                    editor.putString(CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY, new ObjectMapper().writeValueAsString(CONVERSATION_HISTORY));
                    editor.apply();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public static void loadConversationFromFile(AppCompatActivity context) {
        SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
        try {
            ConversationHistory conversationHistory = new ObjectMapper().readValue(sharedPref.getString(CONVERSATION_HISTORY_SHARED_PREFERENCES_KEY, null), ConversationHistory.class);

            if (conversationHistory != null) {
                if (CONVERSATION_HISTORY.getMessageEntries() != null) {
                    CONVERSATION_HISTORY.setMessageEntries(conversationHistory.getMessageEntries());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
