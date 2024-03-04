package com.example.beyond.demo.view.viewpager;

import android.content.Context;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by fishyu on 2017/10/12.
 */

public class SimpleEndlessViewPager extends ViewPager {

    static final String TAG = EndlessViewPager.class.getSimpleName();

    /**
     * The ViewPager
     */
    private static final int ENDLESS_INITIALIZE_FACTOR = 250;

    private boolean mDisableSwipeWhenNotEnoughData = true;

    public SimpleEndlessViewPager(Context context) {
        super(context);
    }

    public SimpleEndlessViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Disable ViewPager's swipe gesture when data is not full ,ie {@link EndlessPagerAdapter#getCountActually()} == 0/1
     *
     * @param disable
     */
    @SuppressWarnings("unused")
    public void setDisableSwipeWhenNotEnoughData(boolean disable) {
        mDisableSwipeWhenNotEnoughData = disable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //If pager count is 0/1 ,disable swipe action
        if (mDisableSwipeWhenNotEnoughData && getAdapter() instanceof EndlessPagerAdapter) {
            if (((EndlessPagerAdapter) getAdapter()).getCountActually() <= 1) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }


    /**
     * Modified for Endless
     * <p>
     * <p>
     * See {@link ViewPager#setCurrentItem(int)} for detail
     *
     * @param item
     */
    @Override
    public void setCurrentItem(int item) {
        setCurrentItem(item, false);
    }

    /**
     * Modified for Endless
     * <p>
     * See {@link ViewPager#setCurrentItem(int, boolean)} for detail
     *
     * @param item
     * @param smoothScroll
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(getEndlessPosition(item), smoothScroll);
    }

    /**
     * Modified for Endless
     * <p>
     * See {@link ViewPager#getCurrentItem()} for detail
     *
     * @return
     */
    @Override
    public int getCurrentItem() {
        if (isEmpty()) {
            return super.getCurrentItem();
        }
        return super.getCurrentItem() % getEndlessAdapter().getCountActually();
    }


    /**
     * Modified for Endless
     * <p>
     * See {@link ViewPager#setAdapter(PagerAdapter)} for detail
     *
     * @return
     */
    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (adapter != null && !(adapter instanceof EndlessPagerAdapter)) {
            throw new IllegalArgumentException("PagerAdapter must be an instance of #EndlessPagerAdapter");
        }
        super.setAdapter(adapter);
    }

    /**
     * Getting endlessed position
     *
     * @param position
     * @return
     */
    public final int getEndlessPosition(int position) {
        if (isEmpty()) {
            return position;
        }

        if (position >= getEndlessAdapter().getCountActually()) {
            // already in endless mode ,just return the position
            return position;
        }

        // If no endless mode
        if (getEndlessAdapter().getCount() == getEndlessAdapter().getCountActually()) {
            return position;
        }

        return ENDLESS_INITIALIZE_FACTOR * getEndlessAdapter().getCountActually() + position;
    }


    /**
     * Initialize ViewPager to EndlessMode
     */
    public void initializeEndless() {
        setCurrentItem(0);
    }


    /**
     * The ViewPager has adapter or not.
     *
     * @return
     */
    private boolean isEmpty() {
        return getAdapter() == null || getAdapter().getCount() == 0;
    }

    /**
     * Getting endless adapter
     *
     * @return
     */
    public EndlessPagerAdapter getEndlessAdapter() {
        if (getAdapter() == null) {
            return null;
        }
        return (EndlessPagerAdapter) getAdapter();
    }


    public abstract static class EndlessPagerAdapter extends FragmentStatePagerAdapter {

        private ViewPager mViewPager;

        public EndlessPagerAdapter(FragmentManager fm, ViewPager viewPager) {
            super(fm);
            if (!(viewPager instanceof EndlessViewPager)) {
                throw new IllegalArgumentException("ViewPager must be an instance of #EndlessViewPager");
            }
            mViewPager = viewPager;
        }

        @Override
        public int getCount() {
            if (getCountActually() <= 1) {
                return getCountActually();
            }
            return 2 * ENDLESS_INITIALIZE_FACTOR * getCountActually();
        }

        /**
         * Returning the actually count
         *
         * @return
         */
        public abstract int getCountActually();


        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }


        /**
         * Getting the actually
         *
         * @param position
         * @return
         */
        protected final int getPositionActually(int position) {
            if (getCount() <= 0) {
                return position;
            }
            return position % getCountActually();
        }

    }


}
