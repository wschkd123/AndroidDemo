package com.example.beyond.demo.view.viewpager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ailong1 on 2017/10/12.<br>
 * <p>
 * This design is A BAD IDEA !!! Dig into ViewPager's source code and override some methods is the supposed way.
 * <p>
 * IDEAS: Add actual-position logic control into an endless-position ViewPager.
 * <p>
 * <p>
 * Concepts:<br>
 * 1, endless-position A very large position for endless swipe<br>
 * 2, actual-position The actual position in Adapter's data list<br>
 * <p>
 * Goals:<br>
 * 1, Endless swipe ViewPager<br>
 * 2, No screen flash when calling {@link PagerAdapter#notifyDataSetChanged()}<br>
 * <p>
 * Classes must be used in pairs:<br>
 * 1, {@link EndlessViewPager} <br>
 * 2, {@link EndlessPagerAdapter}<br>
 * 3, {@link IEndlessFragmentCallback}<br>
 */

public class EndlessViewPager extends ViewPager {

    static final String TAG = EndlessViewPager.class.getSimpleName();

    private static final int ENDLESS_LARGE_COUNT = 100000;
    private static final int ENDLESS_START_COUNT = 10000;


    public EndlessViewPager(Context context) {
        super(context);
    }

    public EndlessViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //If pager count is 0/1 ,disable swipe action
        if (getAdapter() instanceof EndlessPagerAdapter) {
            if (!((EndlessPagerAdapter) getAdapter()).isCanSwipe()) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Modified for Endless. SmoothScroll has been disabled in force.
     * <p>
     * <p>
     * See {@link ViewPager#setCurrentItem(int)} for detail
     *
     * @param item If position is < {@link EndlessPagerAdapter#getCountActually()} , it
     *             would be treated as a actual-position in adapter's data list; If position
     *             >= {@link EndlessPagerAdapter#getCountActually()} ,it would be treated as
     *             endless-position; < 0 would throw {@link IllegalArgumentException}
     */
    @Override
    public void setCurrentItem(int item) {
        setCurrentItem(item, false);
    }


    /**
     * Modified for Endless.
     * <p>
     * Using {@link EndlessViewPager#setCurrentItem(int)} instead.
     * <p>
     * <p>
     * See {@link ViewPager#setCurrentItem(int, boolean)} for detail
     *
     * @param item
     * @param smoothScroll FORCE TO BE FALSE ,THIS PARAMS IS IGNORED.
     */
    @Deprecated
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (!isEmpty()) {
            item = ((EndlessPagerAdapter) getAdapter()).convertPositionToEndless(item);
        }

        if (!isEmpty()) {
            if (item >= getAdapter().getCount() || item < 0) {
                return;
            }
        }

        super.setCurrentItem(item, false);
    }

    /**
     * Using {@link #getCurrentItem(boolean)} instead.
     * <p>
     * Returning actual-position.
     *
     * @return
     */
    @Override
    public int getCurrentItem() {
        return getCurrentItem(true);
    }


    /**
     * Getting current position,result differs from param convertToActualPosition.
     *
     * @param convertToActualPosition false returns super.getCurrentItem ,true convert super.getCurrentItem
     *                                to actual-position.
     * @return
     */
    public int getCurrentItem(boolean convertToActualPosition) {
        if (isEmpty() || !convertToActualPosition) {
            return super.getCurrentItem();
        } else {
            return getEndlessAdapter().convertPositionToActual(super.getCurrentItem());
        }
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


    /**
     * Callback of endless fragment
     */
    public interface IEndlessFragmentCallback {

        void setPosition(int position);

        int getPosition();

        /**
         * When data need updating
         *
         * @param data
         */
        void onDataChanged(Object data);

    }


    /**
     * {@link EndlessViewPager}'s {@link FragmentStatePagerAdapter}.
     */
    public abstract static class EndlessPagerAdapter extends FragmentStatePagerAdapter {

        private List<IEndlessFragmentCallback> mFragments = new ArrayList<>();

        protected EndlessViewPager mViewPager;

        private int mEndlessConverterOffset = 0;
        private int mEndlessConverterActualCount = 0;

        /**
         * Notify current fragment for data changed
         */
        private Handler mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                for (IEndlessFragmentCallback fragment : mFragments) {
                    fragment.onDataChanged(getData(convertPositionToActual(fragment.getPosition())));
                }
            }
        };


        public EndlessPagerAdapter(FragmentManager fm, EndlessViewPager viewPager) {
            super(fm);
            if (!(viewPager instanceof EndlessViewPager)) {
                throw new IllegalArgumentException("ViewPager must be an instance of EndlessViewPager");
            }
            mViewPager = viewPager;
        }


        /**
         * Whether the viewPager can swipe or not.
         *
         * @return
         */
        public boolean isCanSwipe() {
            return getCountActually() > 1;
        }


        @Override
        public int getCount() {
            if (getCountActually() <= 0) {
                return 0;
            }
            return ENDLESS_LARGE_COUNT;
        }

        /**
         * Returning the actually count
         *
         * @return
         */
        public abstract int getCountActually();

        /**
         * Getting data of the position
         *
         * @param actualPosition
         * @return
         */
        public abstract Object getData(int actualPosition);

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object object = super.instantiateItem(container, position);
            if (object instanceof Fragment) {
                if (object instanceof IEndlessFragmentCallback) {
                    ((IEndlessFragmentCallback) object).setPosition(position);
                    mFragments.remove(object);
                    mFragments.add((IEndlessFragmentCallback) object);
                } else {
                    throw new IllegalArgumentException("Fragment must implement EndlessViewPager#IEndlessFragmentCallback");
                }
            }
            return object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            if (object != null) {
                mFragments.remove(object);
            }
        }

        @Override
        public void notifyDataSetChanged() {
            Log.v(TAG, "notifyDataSetChanged getCountActually -> " + getCountActually());
            if (getCountActually() <= 0) {
                Log.v(TAG, "\t no data ,we do nothing");
                super.notifyDataSetChanged();
                initEndlessPositionConverter(0, 0, getCountActually());
                return;
            }
            int currentItem = mViewPager.getCurrentItem(false);
            Log.v(TAG, "viewpager's currentItem -> " + currentItem);
            if (currentItem <= getCountActually()) {
                super.notifyDataSetChanged();
                Log.v(TAG, "\t currentItem is " + currentItem + ", viewpager has not initialize to endless");
                int target = ENDLESS_START_COUNT;
                Log.v(TAG, " target position -> " + target);
                initEndlessPositionConverter(currentItem, target, getCountActually());
                mViewPager.setCurrentItem(target);
            } else {
                int actualPosition = mViewPager.getCurrentItem(true);
                Log.v(TAG, "actualPosition -> " + actualPosition);
                initEndlessPositionConverter(actualPosition, currentItem, getCountActually());
                mHandler.sendEmptyMessage(0);
            }
        }

        /**
         * Initialize the offset for conversation between actual-position and endless-position
         */

        public final void initEndlessPositionConverter(int actualPosition, int endlessPosition, int actualCount) {
            Log.v(TAG, "initEndlessPositionConverter actualPosition -> " + actualPosition + " endlessPosition -> " + endlessPosition + " actualCount -> " + actualCount);
            if (endlessPosition == 0 || actualCount == 0) {
                Log.v(TAG, " 0 ,just set convertParams to 0 and return");
                mEndlessConverterOffset = 0;
                mEndlessConverterActualCount = 0;
                return;
            }

            final int offsetPosition = endlessPosition % actualCount;
            Log.v(TAG, " noOffsetPosition -> " + offsetPosition);

            final int noOffsetPosition = actualPosition;
            Log.v(TAG, " offsetPosition -> " + noOffsetPosition);

            final int offset = offsetPosition - noOffsetPosition;
            Log.v(TAG, " offset -> " + offset);

            mEndlessConverterOffset = offset;
            mEndlessConverterActualCount = actualCount;
        }

        /**
         * Convert endless-position to actual-position.
         *
         * @param endlessPosition
         * @return actual-position, -1 means empty-fragment
         */
        public final int convertPositionToActual(int endlessPosition) {
            Log.v(TAG, "convertPositionToActual -> " + endlessPosition);
            if (!isCanSwipe() && endlessPosition != mViewPager.getCurrentItem(false)) {
                Log.v(TAG, "Set this Fragment to be empty-fragment");
                return -1;
            }

            if (mEndlessConverterActualCount == 0) {
                Log.e(TAG, "\t mEndlessConverterActualCount == 0 ,this should not happen ! return original value -> " + endlessPosition);
                return -1;
            }

            final int offsetPosition = endlessPosition % mEndlessConverterActualCount;
            Log.v(TAG, " offsetPosition -> " + offsetPosition);

            Log.v(TAG, " mEndlessConverterOffset -> " + mEndlessConverterOffset);
            int actualPosition = offsetPosition - mEndlessConverterOffset;

            if (actualPosition < 0) {
                actualPosition += mEndlessConverterActualCount;
            } else if (actualPosition >= mEndlessConverterActualCount) {
                actualPosition -= mEndlessConverterActualCount;
            }

            Log.v(TAG, " return value actualPosition -> " + actualPosition);

            return actualPosition;
        }


        /**
         * Converting actual-position to endless-position
         *
         * @param position If position is < {@link EndlessPagerAdapter#getCountActually()} , it
         *                 would be treated as a actual-position in adapter's data list; If position
         *                 >= {@link EndlessPagerAdapter#getCountActually()} ,it would be treated as
         *                 endless-position; < 0 would throw {@link IllegalArgumentException}
         * @return
         */
        public final int convertPositionToEndless(int position) {
            Log.v(TAG, "convertPositionToEndless -> " + position);
            if (getCountActually() <= 0) {
                Log.v(TAG, " getCountActually <= 0 ,just return position");
                return position;
            }

            if (position < 0) {
                Log.v(TAG, "position is < 0 ,illegal argument !");
                throw new IllegalArgumentException("position must be >= 0");
            }

            if (position > getCountActually()) {
                return position;
            }

            int currentItem = mViewPager.getCurrentItem(false);
            if (currentItem < getCountActually()) {
                Log.v(TAG, " currentItem is too small ,set to ENDLESS_LARGE_COUNT");
                currentItem = ENDLESS_LARGE_COUNT;
            }

            final int currentItemActual = convertPositionToActual(currentItem);
            Log.e(TAG, "\t currentItem -> " + currentItem + " actualPosition -> " + currentItemActual);

            final int startStride = currentItem - currentItemActual;
            Log.v(TAG, " startStride -> " + currentItem);

            int endlessPosition = startStride + position;
            Log.v(TAG, " endlessPosition -> " + endlessPosition);

            if (endlessPosition < currentItem) {
                Log.v(TAG, "\t only move forward !");
                endlessPosition += getCountActually();
            }

            return endlessPosition;
        }
    }

}
