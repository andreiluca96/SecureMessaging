package com.example.andrluc.securemessaging;

import android.content.Context;
import android.util.Log;

import com.example.andrluc.securemessaging.model.ConversationHistory;
import com.example.andrluc.securemessaging.model.MessageEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageReceiver {
    private static final int CONVERSATION_PORT = 12345;
    private static final ConversationHistory CONVERSATION_HISTORY;
    private MessageReceiver() {}

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

    public static void startConversationWriter(final Context context) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    writeToFile(new ObjectMapper().writeValueAsString(CONVERSATION_HISTORY), context);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("conversation.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static void loadConversationFromFile(Context context) {

    }

    private static String readFromFile(Context context) {
        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                bufferedReader.close();
                inputStream.close();

                return bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
