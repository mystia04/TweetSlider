package com.example.mystia04.tweetslider;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
/**
 * https://github.com/workpiles/SwipeableList/blob/master/app/src/main/java/com/example/swipeablelist/SwipeableItemTouchListener.java
 */
public class TweetItemTouchListener implements View.OnTouchListener {
    public static final int LEFT_ON = 0;
    public static final int RIGHT_ON = 1;
    public static final int NEUTRAL = 2;

    private int mSlop;
    private int mSwipingSlop;
    private boolean mSwiping = false;
    private int mAnimationTime;
    private RecyclerView mRecyclerView;
    private Callbacks mCallbacks;
    private TweetItemViewHolder mTouchedView;
    private Point mDownPos = new Point();
    private int mDownViewPos;
    private int mSwitchState = NEUTRAL;
    private int mSwitchThreshold;

    public interface Callbacks {
        void onLeftSw(int viewPos);
        void onRightSw(int viewPos);
    }

    public TweetItemTouchListener(RecyclerView view, Callbacks callbacks) {
        mRecyclerView = view;
        mCallbacks = callbacks;
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mAnimationTime = view.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        mSwitchThreshold = 120;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:

                Rect rect = new Rect();
                int[] listViewCoords = new int[2];
                mRecyclerView.getLocationOnScreen(listViewCoords);
                int x = (int)event.getRawX() - listViewCoords[0];
                int y = (int)event.getRawY() - listViewCoords[1];
                View child;
                for (int i=0;i<mRecyclerView.getChildCount();i++) {
                    child = mRecyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownViewPos = mRecyclerView.getChildAdapterPosition(child);
                        mTouchedView = (TweetItemViewHolder)mRecyclerView.getChildViewHolder(child);
                        mDownPos.set((int)event.getRawX(), (int)event.getRawY());
                        break;
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                if (mTouchedView != null && mSwiping) {
                    mTouchedView.getContentView().animate()
                            .translationX(0)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mDownPos.set(0,0);
                mSwiping = false;
                mSwitchState = NEUTRAL;
                mTouchedView = null;
                break;
            case MotionEvent.ACTION_UP:
                if (!mSwiping) break;
                final int downViewPos = mDownViewPos;
                final int switchState = mSwitchState;
                mTouchedView.getContentView().animate()
                        .translationX(0)
                        .setDuration(mAnimationTime)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (switchState == LEFT_ON) {
                                    mCallbacks.onLeftSw(downViewPos);
                                } else if (switchState == RIGHT_ON) {
                                    mCallbacks.onRightSw(downViewPos);
                                }
                            }
                        });

                mDownPos.set(0,0);
                mSwiping = false;
                mSwitchState = NEUTRAL;
                mTouchedView = null;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - mDownPos.x;
                float deltaY = event.getRawY() - mDownPos.y;
                if (mSwiping) {
                    if (Math.abs(deltaX - mSwipingSlop) < mSwitchThreshold) {
                        mSwitchState = (deltaX > 0 ? LEFT_ON : RIGHT_ON);
                        //mSwitchState = NEUTRAL;
                        mTouchedView.getContentView().setTranslationX(deltaX - mSwipingSlop);
                    }
                    mTouchedView.setBackgroundView(mSwitchState);
                    return true;

                } else {
                    if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                        mSwiping = true;
                        mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                        mSwitchState = (deltaX > 0 ? LEFT_ON : RIGHT_ON);
                        //mSwitchState = NEUTRAL;
                        mRecyclerView.requestDisallowInterceptTouchEvent(true);

                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mRecyclerView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }
                }

                break;
        }
        return false;
    }
}