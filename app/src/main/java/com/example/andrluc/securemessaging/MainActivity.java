package com.example.andrluc.securemessaging;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private List<String> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for(BluetoothDevice bt : pairedDevices)
            users.add(bt.getName());

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
        boolean isFirstRun = wmbPreference.getBoolean("firstGo", true);
        if (isFirstRun)
        {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();

                RSAPublicKey publicKey = (RSAPublicKey)kp.getPublic();
                String publicKeyString = publicKey.getModulus().toString() + "|" + publicKey.getPublicExponent().toString();

                final PublicKeyEntry publicKeyEntry = new PublicKeyEntry();
                publicKeyEntry.setUsername(mBluetoothAdapter.getName());
                publicKeyEntry.setPublicKey(publicKeyString);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
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
