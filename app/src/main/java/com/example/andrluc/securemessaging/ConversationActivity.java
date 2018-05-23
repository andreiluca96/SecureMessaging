package com.example.andrluc.securemessaging;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.andrluc.securemessaging.model.MessageEntry;
import com.example.andrluc.securemessaging.model.MessageEntryDTO;
import com.example.andrluc.securemessaging.utils.ConversationUtil;
import com.example.andrluc.securemessaging.utils.CryptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class ConversationActivity extends AppCompatActivity {
    private final int CONVERSATION_PORT = 12345;

    private String hostIPAddress;
    private String selfIPAddress;
    private List<Message> messages = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        this.hostIPAddress = getIntent().getStringExtra("HOST");
        this.selfIPAddress = getIntent().getStringExtra("SELF");

        RecyclerView recyclerView = findViewById(R.id.reyclerview_message_list);
        recyclerView.setHasFixedSize(true);

        MessageListAdapter messageListAdapter = new MessageListAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageListAdapter);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                messages = new ArrayList<>();

                List<MessageEntry> messageEntries = ConversationUtil.getConversationHistory().getMessageEntries();
                List<MessageEntry> filteredMessageEntries = new ArrayList<>();

                for (MessageEntry messageEntry : messageEntries) {
                    if (messageEntry.getSender().equals(hostIPAddress) || messageEntry.getSender().equals(hostIPAddress)) {
                        filteredMessageEntries.add(messageEntry);
                    }
                }

                Collections.sort(filteredMessageEntries, (messageEntry, t1) -> {
                            if (messageEntry.getDate().equals(t1.getDate())) {
                                return 0;
                            }

                            if (messageEntry.getDate().before(t1.getDate())) {
                                return -1;
                            } else {
                                return 1;
                            }
                        });

                for (MessageEntry messageEntry : filteredMessageEntries) {
                    Message message = new Message();
                    message.setSender(messageEntry.getSender());
                    message.setMessage(messageEntry.getMessage());
                    message.setCreatedAt(messageEntry.getDate());

                    messages.add(message);
                }

                MessageListAdapter messageListAdapter1 = new MessageListAdapter(messages);
                System.out.println("Adaptam...");

                runOnUiThread(() -> {
                    recyclerView.swapAdapter(messageListAdapter1, false);
                    recyclerView.smoothScrollToPosition(messages.size());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0, 10, TimeUnit.SECONDS);

        messageListAdapter = new MessageListAdapter(messages);
        recyclerView.swapAdapter(messageListAdapter, false);
    }

    public void sendMessage(View view) {
        new Thread(() -> {
            EditText editText = findViewById(R.id.edittext_chatbox);
            String message = editText.getText().toString();

            runOnUiThread(() -> editText.setText(""));

            Date date = new Date();

            MessageEntry messageEntry = new MessageEntry();
            messageEntry.setSender(selfIPAddress);
            messageEntry.setReceiver(hostIPAddress);
            messageEntry.setDate(date);
            messageEntry.setMessage(message);
            ConversationUtil.getConversationHistory().getMessageEntries().add(messageEntry);

            SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
            String privateKeyString= wmbPreference.getString("privateKey", null);
            assert privateKeyString != null;
            String[] split = privateKeyString.split("\\|");

            BigInteger modulus = new BigInteger(split[0]);
            BigInteger exponent = new BigInteger(split[1]);

            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
                PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256); // for example
                SecretKey secretKey = keyGen.generateKey();

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                IvParameterSpec spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
                byte[] iv = spec.getIV();

                MessageEntryDTO messageEntryDTO = new MessageEntryDTO();
                messageEntryDTO.setSender(selfIPAddress);
                messageEntryDTO.setReceiver(hostIPAddress);
                messageEntryDTO.setDate(date);
                messageEntryDTO.setIv(iv);
                messageEntryDTO.setEncryptionKey(CryptoUtil.encrypt(privateKey, secretKey.getEncoded()));
                messageEntryDTO.setEncryptedMessage(cipher.doFinal(message.getBytes()));

                Socket socket = new Socket(hostIPAddress, CONVERSATION_PORT);
                OutputStream outputStream = socket.getOutputStream();

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println(new ObjectMapper().writeValueAsString(messageEntryDTO));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public class MessageListAdapter extends RecyclerView.Adapter {
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        private List<Message> mMessageList;

        public MessageListAdapter(List<Message> messageList) {
            mMessageList = messageList;
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        @Override
        public int getItemViewType(int position) {
            Message message = mMessageList.get(position);

            if (message.getSender().equals(selfIPAddress)) {
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageHolder(view);
            }

            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Message message = mMessageList.get(position);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        public void notify(List<Message> list) {
            if (list != null) {
                this.mMessageList.clear();
                this.mMessageList.addAll(list);

            }
            notifyDataSetChanged();
        }

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(Message message) {
                messageText.setText(message.getMessage());
                timeText.setText(message.getCreatedAt().toString());
            }
        }

        private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, nameText;
            ImageView profileImage;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
                nameText = itemView.findViewById(R.id.text_message_name);
                profileImage = itemView.findViewById(R.id.image_message_profile);
            }

            void bind(Message message) {
                messageText.setText(message.getMessage());
                timeText.setText(message.getCreatedAt().toString());
                nameText.setText(message.getSender());
        }
        }
    }
    class Message {
        private String message;
        private String sender;
        private Date createdAt;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }
}
