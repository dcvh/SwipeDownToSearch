package com.example.cpu10661.pulldownsearch;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by cpu10661 on 12/27/17.
 */

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatHolder>{

    private int mChatSize;
    private Context mContext;

    public ChatListAdapter(int chatSize) {
        if (chatSize <= 0) {
            chatSize = 10;
        }
        this.mChatSize = chatSize;
    }

    @Override
    public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.chat_row_item, parent, false);
        return new ChatHolder(view);
    }

    private static final String mNamePrefix = "Nguyen Van ";
    @Override
    public void onBindViewHolder(ChatHolder holder, int position) {
        holder.mAvatarImageView.setImageResource(R.drawable.profile_picture_placeholder);
        holder.mNameTextView.setText(mNamePrefix + String.valueOf(position + 1));
        holder.mLastMessageTextView.setText(mContext.getString(R.string.nothing_yet));
    }

    @Override
    public int getItemCount() {
        return mChatSize;
    }

    class ChatHolder extends RecyclerView.ViewHolder {

        CircleImageView mAvatarImageView;
        TextView mNameTextView;
        TextView mLastMessageTextView;

        public ChatHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.iv_profile_picture);
            mNameTextView = itemView.findViewById(R.id.tv_name);
            mLastMessageTextView = itemView.findViewById(R.id.tv_last_message);
        }
    }
}
