package com.example.andrluc.securemessaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<String> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        users.add("user1");
        users.add("user2");
        users.add("user3");
        users.add("user4");
        users.add("user5");
        users.add("user6");
        users.add("user7");
        users.add("user8");
        users.add("user9");

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


        SharedPreferences wmbPreference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = wmbPreference.getBoolean("firstTime", true);
        if (isFirstRun)
        {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String address = manager.getConnectionInfo().getMacAddress();

            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();

                RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
                String publicKeyString = publicKey.getModulus().toString() + "|" + publicKey.getPublicExponent().toString();

                final PublicKeyEntry publicKeyEntry = new PublicKeyEntry();
                publicKeyEntry.setMacAddress(address);
                publicKeyEntry.setPublicKey(publicKeyString);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DynamoDBUtil.getDynamoDBMapper().save(publicKeyEntry);
                    }
                }).start();

                SharedPreferences.Editor editor = wmbPreference.edit();
                editor.putBoolean("firstTime", false);
                RSAPrivateKey privateKey = (RSAPrivateKey)kp.getPrivate();
                editor.putString("privateKey", privateKey.getModulus().toString() + "|" + privateKey.getPrivateExponent().toString());
                editor.apply();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


        }
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
