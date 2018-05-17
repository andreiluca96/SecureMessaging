package com.example.andrluc.securemessaging;

import android.content.Context;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {
    private final int CONVERSATION_PORT = 12345;

    private String receiverIPAddress = "127.0.0.1";
    private String senderIPAddress = "127.0.0.1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        RecyclerView recyclerView = findViewById(R.id.reyclerview_message_list);
        recyclerView.setHasFixedSize(true);

        List<Message> messages = new ArrayList<>();
        Message dummy = new Message();
        dummy.setCreatedAt(10);
        dummy.setMessage("MESSAGEEEEE");
        User sender = new User();
        sender.setNickname("John Doe");
        sender.setProfileUrl("John Doe");
        dummy.setSender(sender);

        messages.add(dummy);

        Message dummy2 = new Message();
        dummy2.setCreatedAt(10);
        dummy2.setMessage("MESSAGEEEEE");
        User sender2 = new User();
        sender.setProfileUrl("John Doe");
        dummy2.setSender(sender2);

        messages.add(dummy2);

        MessageListAdapter messageListAdapter = new MessageListAdapter(this, messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageListAdapter);
    }

    public void sendMessage(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MessageEntry messageEntry = new MessageEntry();
                String message = ((EditText)findViewById(R.id.edittext_chatbox)).getText().toString();

                messageEntry.setSender(senderIPAddress);
                messageEntry.setReceiver(receiverIPAddress);
                messageEntry.setDate(new Date());
                messageEntry.setMessage(message);

                try {
                    Socket socket = new Socket(receiverIPAddress, CONVERSATION_PORT);
                    OutputStream outputStream = socket.getOutputStream();

                    PrintWriter writer = new PrintWriter(outputStream, true);

                    writer.println(new ObjectMapper().writeValueAsString(messageEntry));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public class MessageListAdapter extends RecyclerView.Adapter {
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        private Context mContext;
        private List<Message> mMessageList;

        public MessageListAdapter(Context context, List<Message> messageList) {
            mContext = context;
            mMessageList = messageList;
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        // Determines the appropriate ViewType according to the sender of the message.
        @Override
        public int getItemViewType(int position) {
            Message message = mMessageList.get(position);

            if (message.getSender().getNickname() == null) {
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        // Inflates the appropriate layout according to the ViewType.
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

        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Message message = (Message) mMessageList.get(position);

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

//                timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
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

                // Format the stored timestamp into a readable String using method.
//                timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
                timeText.setText("date");

                nameText.setText(message.getSender().getNickname());

                // Insert the profile image from the URL into the ImageView.
//                Utils.displayRoundImageFromUrl(mContext, message.getSender().getProfileUrl(), profileImage);
            }
        }
    }
    class Message {
        private String message;
        private User sender;
        private long createdAt;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public User getSender() {
            return sender;
        }

        public void setSender(User sender) {
            this.sender = sender;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
    }
    class User {
        private String nickname;
        private String profileUrl;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getProfileUrl() {
            return profileUrl;
        }

        public void setProfileUrl(String profileUrl) {
            this.profileUrl = profileUrl;
        }
    }
}
