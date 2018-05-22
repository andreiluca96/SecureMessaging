package com.example.andrluc.securemessaging;

import android.os.Build;
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
import com.example.andrluc.securemessaging.utils.ConversationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            messages = new ArrayList<>();

            ConversationUtil.getConversationHistory()
                    .getMessageEntries()
                    .stream()
                    .filter(messageEntry -> messageEntry.getSender().equals(hostIPAddress) || messageEntry.getSender().equals(hostIPAddress))
                    .sorted((messageEntry, t1) -> {
                        if (messageEntry.getDate().equals(t1.getDate())) {
                            return 0;
                        }

                        if (messageEntry.getDate().before(t1.getDate())) {
                            return 1;
                        } else {
                            return -1;
                        }
                    })
                    .forEach(messageEntry -> {
                        Message message = new Message();
                        message.setSender(messageEntry.getSender());
                        message.setMessage(messageEntry.getMessage());
                        message.setCreatedAt(messageEntry.getDate());

                        messages.add(message);
                    });
        }, 0, 10, TimeUnit.SECONDS);

        Message dummy = new Message();
        dummy.setCreatedAt(new Date());
        dummy.setMessage("MESSAGEEEEE");
        dummy.setSender("John Doe");

        messages.add(dummy);

        Message dummy2 = new Message();
        dummy2.setCreatedAt(new Date());
        dummy2.setMessage("MESSAGEEEEE");
        dummy2.setSender("John Doe");

        messages.add(dummy2);

        MessageListAdapter messageListAdapter = new MessageListAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageListAdapter);
    }

    public void sendMessage(View view) {
        new Thread(() -> {
            MessageEntry messageEntry = new MessageEntry();
            String message = ((EditText)findViewById(R.id.edittext_chatbox)).getText().toString();

            messageEntry.setSender(selfIPAddress);
            messageEntry.setReceiver(hostIPAddress);
            messageEntry.setDate(new Date());
            messageEntry.setMessage(message);

            try {
                Socket socket = new Socket(hostIPAddress, CONVERSATION_PORT);
                OutputStream outputStream = socket.getOutputStream();

                PrintWriter writer = new PrintWriter(outputStream, true);

                writer.println(new ObjectMapper().writeValueAsString(messageEntry));
            } catch (IOException e) {
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

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.text_message_body);
                timeText = itemView.findViewById(R.id.text_message_time);
            }

            void bind(Message message) {
                messageText.setText(message.getMessage());
                timeText.setText("date");
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
