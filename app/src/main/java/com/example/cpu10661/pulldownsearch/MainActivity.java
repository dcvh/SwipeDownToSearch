package com.example.cpu10661.pulldownsearch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeChatList();
    }

    private void initializeChatList() {
        RecyclerView chatListRecyclerView = findViewById(R.id.rv_chat_list);
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatListRecyclerView.setItemAnimator(new DefaultItemAnimator());

        ChatListAdapter adapter = new ChatListAdapter(20);
        chatListRecyclerView.setAdapter(adapter);
    }
}