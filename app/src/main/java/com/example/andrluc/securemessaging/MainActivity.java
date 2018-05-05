package com.example.andrluc.securemessaging;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
