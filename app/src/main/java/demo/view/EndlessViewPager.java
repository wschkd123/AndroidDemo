package demo.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fishyu on 2017/10/12.<br>
 * <p>
 * <p>
 * This design is not perfect ,dig into ViewPager's source code and override some methods is the supposed way
 * <p>
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

    private static final int ENDLESS_INITIALIZE_FACTOR = 250;


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
     * Modified for Endless
     * <p>
     * <p>
     * See {@link ViewPager#setCurrentItem(int)} for detail
     *
     * @param item
     */
    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }

    /**
     * Modified for Endless
     * <p>
     * See {@link ViewPager#setCurrentItem(int, boolean)} for detail
     *
     * @param item
     * @param smoothScroll FORCE TO BE FORCE ,THIS PARAMS IS IGNORED.
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, false);
    }

    @Deprecated
    @Override
    public int getCurrentItem() {
        return super.getCurrentItem();
    }

    /**
     * Getting actualCurrentItem ,{@link #getCurrentItem()} returns the endless-position
     *
     * @return
     */
    public int getActualCurrentItem() {
        if (isEmpty()) {
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

        void onDataChanged(Object data);

    }


    /**
     * {@link EndlessViewPager}'s {@link FragmentStatePagerAdapter}.
     */
    public abstract static class EndlessPagerAdapter extends FragmentStatePagerAdapter {

        private List<IEndlessFragmentCallback> mFragments = new ArrayList<>();

        private EndlessViewPager mViewPager;

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
            return 10000;
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

            int currentItem = mViewPager.getCurrentItem();
            Log.v(TAG, "viewpager's currentItem -> " + currentItem);

            if (currentItem <= getCountActually()) {
                super.notifyDataSetChanged();
                Log.v(TAG, "\t currentItem is " + currentItem + ", viewpager has not initialize to endless");
                int target = 2000;
                Log.v(TAG, " target position -> " + target);
                initEndlessPositionConverter(currentItem, target, getCountActually());
                mViewPager.setCurrentItem(target);
            } else {
                int actualPosition = mViewPager.getActualCurrentItem();
                Log.v(TAG, "actualPosition -> " + actualPosition);
                initEndlessPositionConverter(actualPosition, currentItem, getCountActually());

//                //update currentItems
//                for (IEndlessFragmentCallback fragment : mFragments) {
//                    fragment.onDataChanged(getData(convertPositionToActual(fragment.getPosition())));
//                }
                mHandler.sendEmptyMessage(0);

            }
        }

        private int mEndlessConverterOffset = 0;

        /**
         * Initialize the offset for conversation between actual-position and endless-position
         */
        public final void initEndlessPositionConverter(int actualPosition, int endlessPosition, int actualCount) {
            Log.v(TAG, "initEndlessPositionConverter actualPosition -> " + actualPosition + " endlessPosition -> " + endlessPosition + " actualCount -> " + actualCount);

            if (endlessPosition == 0 || actualCount == 0) {
                Log.v(TAG, " 0 ,just set convertParams to 0 and return");
                mEndlessConverterOffset = 0;
                return;
            }

            final int noOffsetPosition = endlessPosition % actualCount;
            Log.v(TAG, " noOffsetPosition -> " + noOffsetPosition);

            final int offsetPosition = actualPosition;
            Log.v(TAG, " offsetPosition -> " + offsetPosition);

            final int offset = offsetPosition - noOffsetPosition;
            Log.v(TAG, " offset -> " + offset);

            mEndlessConverterOffset = offset;
        }

        /**
         * Convert the position from endless-position to actual-position
         *
         * @param endlessPosition The endless-position ,ie the large number of the current position
         * @return Actual position
         */
        public final int convertPositionToActual(int endlessPosition) {
            Log.v(TAG, "convertPositionToActual -> " + endlessPosition);
            if (endlessPosition < getCountActually()) {
                Log.v(TAG, " endlessPosition < getCountActually(), actualPosition -> " + endlessPosition);
                return endlessPosition;
            }

            final int offsetPosition = endlessPosition % getCountActually();
            Log.v(TAG, " offsetPosition -> " + offsetPosition);

            final int actualPosition = offsetPosition - mEndlessConverterOffset;
            Log.v(TAG, " return value actualPosition -> " + actualPosition);

            return actualPosition;
        }

    }
}
