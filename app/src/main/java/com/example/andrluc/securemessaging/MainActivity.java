package com.example.andrluc.securemessaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.andrluc.securemessaging.model.ContactItem;
import com.example.andrluc.securemessaging.model.MessageEntry;
import com.example.andrluc.securemessaging.model.PublicKeyEntry;
import com.example.andrluc.securemessaging.utils.ConversationUtil;
import com.example.andrluc.securemessaging.utils.DynamoDBUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String SUBNET_MASK = "192.168.100";

    private final List<String> users = new ArrayList<>();
    private final List<ContactItem> contactsLists = new ArrayList<>();
    private String ipAddress;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConversationUtil.startConversationReceiver();

        ConversationUtil.loadConversationFromFile(this);

        ConversationUtil.startConversationWriter(this);

        users.addAll(checkHosts(SUBNET_MASK));

        WifiManager wm = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wm != null;
        WifiInfo connectionInfo = wm.getConnectionInfo();

        int ipAddress = connectionInfo.getIpAddress();
        this.ipAddress = Formatter.formatIpAddress(ipAddress);
        System.out.println("HostIP: " + this.ipAddress);

        buildContactList();

        ListView conversationListView = findViewById(R.id.conversationListView);
        ConversationItemAdapter conversationItemAdapter = new ConversationItemAdapter();
        conversationListView.setAdapter(conversationItemAdapter);

        conversationListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            intent.putExtra("HOST", contactsLists.get(i).getHostName());
            intent.putExtra("SELF", this.ipAddress);

            startActivity(intent);
        });

        publishToDDBPublicKeyOnce();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void buildContactList() {
        for (String user : users) {
            ContactItem contactItem = new ContactItem();
            contactItem.setHostName(user);

            List<MessageEntry> filteredEntries = new ArrayList<>();
            List<MessageEntry> messageEntries = ConversationUtil.getConversationHistory().getMessageEntries();


            for (MessageEntry messageEntry : messageEntries) {
                if (messageEntry.getSender().equals(user) || messageEntry.getReceiver().equals(user)) {
                    filteredEntries.add(messageEntry);
                }
            }

            Collections.sort(filteredEntries, (messageEntry, t1) -> {
                if (messageEntry.getDate().equals(t1.getDate())) {
                    return 0;
                }

                if (messageEntry.getDate().before(t1.getDate())) {
                    return 1;
                } else {
                    return -1;
                }
            });

            if (filteredEntries.size() > 0) {
                MessageEntry messageEntry = filteredEntries.get(0);
                contactItem.setTimestamp(messageEntry.getDate());

                String message;

                if (messageEntry.getSender().equals(this.ipAddress)) {
                    message = "You: " + messageEntry.getMessage();
                } else {
                    message = user + ": " + messageEntry.getMessage();
                }
                contactItem.setMessage(message);
            } else {
                contactItem.setMessage("No messages yet.");
            }

            contactsLists.add(contactItem);
        }
    }

    private void publishToDDBPublicKeyOnce() {
        SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = wmbPreference.getBoolean(ipAddress, true);
        if (isFirstRun)
        {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();

                RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
                final String publicKeyString = publicKey.getModulus().toString() + "|" + publicKey.getPublicExponent().toString();

                new Thread(() -> {
                    final PublicKeyEntry publicKeyEntry = new PublicKeyEntry();

                    publicKeyEntry.setUsername(this.ipAddress);
                    publicKeyEntry.setPublicKey(publicKeyString);


                    DynamoDBUtil.getDynamoDBMapper().save(publicKeyEntry);
                }).start();

                SharedPreferences.Editor editor = wmbPreference.edit();
                editor.putBoolean(ipAddress, false);
                RSAPrivateKey privateKey = (RSAPrivateKey)kp.getPrivate();
                editor.putString("privateKey", privateKey.getModulus().toString() + "|" + privateKey.getPrivateExponent().toString());
                editor.apply();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> checkHosts(final String subnet){
        final List<String> hosts = new ArrayList<>();

        Thread thread = new Thread(() -> {
            int timeout = 10;
            for (int i = 1; i < 255; i++) {
                String host = subnet + "." + i;
                try {
                    System.out.println(i);

                    if (InetAddress.getByName(host).isReachable(timeout)) {
                        hosts.add(host);
                        System.out.println("Found" + host);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return hosts;
    }

    class ConversationItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return contactsLists.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @SuppressLint({"ViewHolder", "InflateParams"})
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.conversation_list_view_item, null);

            TextView usernameTextView = view.findViewById(R.id.usernameTextView);
            TextView lastMessageTextView = view.findViewById(R.id.lastMessageTextView);
            TextView lastMessageDateTextView = view.findViewById(R.id.lastMessageDateTextView);

            usernameTextView.setText(contactsLists.get(i).getHostName());
            lastMessageTextView.setText(contactsLists.get(i).getMessage());
            lastMessageDateTextView.setText(contactsLists.get(i).getTimestamp() == null? "" : contactsLists.get(i).getTimestamp().toString());

            return view;
        }
    }
}
