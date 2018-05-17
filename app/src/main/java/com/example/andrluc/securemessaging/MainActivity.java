package com.example.andrluc.securemessaging;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.andrluc.securemessaging.model.PublicKeyEntry;
import com.example.andrluc.securemessaging.utils.DynamoDBUtil;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String SUBNET_MASK = "192.168.1";
    private List<String> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        users.addAll(checkHosts(SUBNET_MASK));

        ListView conversationListView = findViewById(R.id.conversationListView);
        ConversationItemAdapter conversationItemAdapter = new ConversationItemAdapter();
        conversationListView.setAdapter(conversationItemAdapter);

        conversationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                startActivity(intent);
            }
        });

        publishToDDBPublicKeyOnce();
    }

    private void publishToDDBPublicKeyOnce() {
        SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = wmbPreference.getBoolean("firstGo", true);
        if (isFirstRun)
        {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();

                RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
                final String publicKeyString = publicKey.getModulus().toString() + "|" + publicKey.getPublicExponent().toString();



                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final PublicKeyEntry publicKeyEntry = new PublicKeyEntry();

                        try {
                            publicKeyEntry.setUsername(Inet4Address.getLocalHost().getHostAddress());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        publicKeyEntry.setPublicKey(publicKeyString);


                        DynamoDBUtil.getDynamoDBMapper().save(publicKeyEntry);
                    }
                }).start();

                SharedPreferences.Editor editor = wmbPreference.edit();
                editor.putBoolean("firstGo", false);
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

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int timeout = 50;
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
            return users.size();
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

            usernameTextView.setText(users.get(i));
            lastMessageTextView.setText(users.get(i));
            lastMessageDateTextView.setText("some date");

            return view;
        }
    }
}
