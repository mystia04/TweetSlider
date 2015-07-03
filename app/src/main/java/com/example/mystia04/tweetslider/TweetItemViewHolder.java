package com.example.mystia04.tweetslider;

/**
 * 
 */
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.loopj.android.image.SmartImageView;
import java.util.ArrayList;
import twitter4j.Status;

public class TweetItemViewHolder extends RecyclerView.ViewHolder {
    public static final int LEFT_ON = 0;
    public static final int RIGHT_ON = 1;

    private View mContentView;
    private ArrayList<View> mBackgrounds;

    public TweetItemViewHolder(View itemView) {
        super(itemView);
        mContentView = itemView.findViewById(R.id.itemContent);
        mBackgrounds = new ArrayList<View>();
        mBackgrounds.add(LEFT_ON, itemView.findViewById(R.id.itemLeftOn));
        mBackgrounds.add(RIGHT_ON, itemView.findViewById(R.id.itemRightOn));
        for (View v : mBackgrounds) v.setVisibility(View.INVISIBLE);

    }

    public View getContentView() {
        return mContentView;
    }

    public void setBackgroundView(int pos) {
        for (int i=0;i<mBackgrounds.size();i++) {
            if (pos == i) {
                mBackgrounds.get(i).setVisibility(View.VISIBLE);
            } else {
                mBackgrounds.get(i).setVisibility(View.INVISIBLE);
            }
        }
    }

    public void bindData(Status data) {
        Status item = data;
        TextView name = (TextView) mContentView.findViewById(R.id.name);
        name.setText(item.getUser().getName());
        TextView screenName = (TextView) mContentView.findViewById(R.id.screen_name);
        screenName.setText("@" + item.getUser().getScreenName());
        SmartImageView icon = (SmartImageView) mContentView.findViewById(R.id.icon);
        icon.setImageUrl(item.getUser().getProfileImageURL());
        TextView text = (TextView) mContentView.findViewById(R.id.text);
        text.setText(item.getText());
    }
}