package com.example.mystia04.tweetslider;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.image.SmartImageView;

import java.util.ArrayList;
import java.util.List;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;


public class MainActivity extends ListActivity {

	private TweetAdapter mAdapter;
	private Twitter mTwitter;
	private SwipeRefreshLayout hSwipeRefreshLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timeline_layout);

		/**
		 * SwipeRefreshLayoutのハンドラを取得
		 * findViewByIdはsetContentView実行後でないとnullが返される
		 */
		hSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
		//	Pull to Refreshで引っ張られたときの動作を定義
		hSwipeRefreshLayout.setOnRefreshListener(hOnSwipeRefreshListener);

		/* トークン持ってるか判別 */
		if (!TwitterUtils.hasAccessToken(this)) {
			Intent intent = new Intent(this, TwitterOAuthActivity.class);
			startActivity(intent);
			finish();
		} else {
			mAdapter = new TweetAdapter(this);
			setListAdapter(mAdapter);

			mTwitter = TwitterUtils.getTwitterInstance(this);
			reloadTimeLine();
		}
	}

	private SwipeRefreshLayout.OnRefreshListener hOnSwipeRefreshListener = new SwipeRefreshLayout.OnRefreshListener()
	{
		@Override
		public void onRefresh()
		{
			reloadTimeLine();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				reloadTimeLine();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class TweetAdapter extends ArrayAdapter<Status> {

		private LayoutInflater mInflater;

		public TweetAdapter(Context context) {
			super(context, R.layout.timeline_layout);
			mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item_tweet, null);
			}
			Status item = getItem(position);
			TextView name = (TextView) convertView.findViewById(R.id.name);
			name.setText(item.getUser().getName());
			TextView screenName = (TextView) convertView.findViewById(R.id.screen_name);
			screenName.setText("@" + item.getUser().getScreenName());
			TextView text = (TextView) convertView.findViewById(R.id.text);
			text.setText(item.getText());
			SmartImageView icon = (SmartImageView) convertView.findViewById(R.id.icon);
			icon.setImageUrl(item.getUser().getProfileImageURL());
			return convertView;
		}
	}

	private void reloadTimeLine() {
		AsyncTask<Void, Void, List<Status>> task = new AsyncTask<Void, Void, List<Status>>() {
			@Override
			protected List<twitter4j.Status> doInBackground(Void... params) {
				try {
					return mTwitter.getHomeTimeline();
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(List<twitter4j.Status> result) {
				if (result != null) {
					mAdapter.clear();
					for (twitter4j.Status status : result) {
						mAdapter.add(status);
					}
					getListView().setSelection(0);
				} else {
					showToast("タイムラインの取得に失敗しました。。。");
				}
				//	Pull to Refreshの動作を終了
				hSwipeRefreshLayout.setRefreshing(false);
			}
		};
		task.execute();
	}

	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
}


