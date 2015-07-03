package com.example.mystia04.tweetslider;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.LinearLayoutManager;
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


public class MainActivity extends Activity {

	private TweetAdapter mAdapter;
	private Twitter mTwitter;
	private SwipeRefreshLayout hSwipeRefreshLayout;
	private RecyclerView.LayoutManager hLayoutManager;
	private RecyclerView hRecyclerView;

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

		hRecyclerView = (RecyclerView)findViewById(R.id.recyclerview);
		hLayoutManager = new LinearLayoutManager(this);
		hRecyclerView.setLayoutManager(hLayoutManager);


		hRecyclerView.setOnTouchListener(new TweetItemTouchListener(hRecyclerView,
				new TweetItemTouchListener.Callbacks() {
					@Override
					public void onLeftSw(int viewPos) {
						//	Favorite
						createFavorite(mAdapter.getStatusFromPosition(viewPos).getId());
					}

					@Override
					public void onRightSw(int viewPos) {
						//	Retweet
						createRetweet(mAdapter.getStatusFromPosition(viewPos).getId());
					}
				}));

		/* トークン持ってるか判別 */
		if (!TwitterUtils.hasAccessToken(this)) {
			Intent intent = new Intent(this, TwitterOAuthActivity.class);
			startActivity(intent);
			finish();
		} else {
			mAdapter = new TweetAdapter(this);
			hRecyclerView.setAdapter(mAdapter);
			//setListAdapter(mAdapter);

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

	private class TweetAdapter extends RecyclerView.Adapter<TweetItemViewHolder> {
		private LayoutInflater mInflater;
		private ArrayList<Status> tweetList;

		public TweetAdapter(Context context) {
			super();
			tweetList = new ArrayList<Status>();
			mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public TweetItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
			View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_tweet, null, false);
			return new TweetItemViewHolder(v);
		}

		@Override
		public void onBindViewHolder(TweetItemViewHolder viewHolder, int i){
			viewHolder.bindData(tweetList.get(i));
		}

		@Override
		public int getItemCount() {
			return tweetList.size();
		}

		public Status getStatusFromPosition(int pos)
		{
			return tweetList.get(pos);
		}

		public void remove(int pos) {
			if ( pos < 0 )
				return;
			tweetList.remove(pos);
			notifyItemRemoved(pos);
		}

		public void add(int pos, Status data) {
			tweetList.add(pos, data);
			notifyItemInserted(pos);
		}

		public void clear()
		{
			int len = tweetList.size();
			tweetList.clear();
			notifyItemRangeRemoved(0,len);
		}
		/*
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
		*/
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
					int i = 0;
					for (twitter4j.Status status : result) {
						mAdapter.add(i, status);
						i ++;
					}
					//getListView().setSelection(0);
				} else {
					showToast("タイムラインの取得に失敗しました。。。");
				}
				//	Pull to Refreshの動作を終了
				hSwipeRefreshLayout.setRefreshing(false);
			}
		};
		task.execute();
	}

	/**
	 * お気に入りに登録します
	 * @param status_id 登録するstatus_id
	 */
	private void createFavorite(long status_id) {
		AsyncTask<Long, Void, twitter4j.Status> task = new AsyncTask<Long, Void, twitter4j.Status>() {
			@Override
			protected twitter4j.Status doInBackground(Long... params) {
				try {
					return mTwitter.createFavorite((long) params[0]);
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}
			@Override
			protected void onPostExecute(twitter4j.Status result) {
				if (result == null) {
					showToast("お気に入り登録に失敗しました…");
				}else{
					showToast("お気に入りに登録しました！");
				}
			}
		};
		task.execute(status_id);
	}

	/**
	 * リツイートを行います
	 * @param status_id 登録するstatus_id
	 */
	private void createRetweet(long status_id) {
		AsyncTask<Long, Void, twitter4j.Status> task = new AsyncTask<Long, Void, twitter4j.Status>() {
			@Override
			protected twitter4j.Status doInBackground(Long... params) {
				try {
					return mTwitter.retweetStatus((long)params[0]);
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}
			@Override
			protected void onPostExecute(twitter4j.Status result) {
				if (result == null) {
					showToast("リツイートに失敗しました…");
				}else {
					showToast("リツイートしました！");
				}
			}
		};
		task.execute(status_id);
	}

	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
}


