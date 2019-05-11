package com.github.fakegps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.github.fakegps.model.LocPoint;
import com.github.fakegps.ui.BookmarkActivity;
import com.github.fakegps.ui.JoyStickView;
import com.github.fakegps.ui.MainActivity;
import com.tencent.fakegps.R;

import tiger.radio.loggerlibrary.Logger;

/**
 * Created by tiger on 7/22/16.
 * Switched to GPSd by Sven@Killig.de on 2019/05/11
 */
public class JoyStickManager implements IJoyStickPresenter {

    private static final String TAG = "JoyStickManager";

    public static double STEP_DEFAULT = 0.00002;

    private static JoyStickManager INSTANCE = new JoyStickManager();

    private Context mContext;
    private LocationThread mLocationThread;
    private boolean mIsStarted = false;
    private double mMoveStep = STEP_DEFAULT;

    private LocPoint mCurrentLocPoint;

    private LocPoint mTargetLocPoint;
    private int mFlyTime;
    private int mFlyTimeIndex;
    private boolean mIsFlyMode = false;

    private JoyStickView mJoyStickView;

    private JoyStickManager() {
    }


    public void init(Context context) {
        mContext = context;
    }

    public static JoyStickManager get() {
        return INSTANCE;
    }

    public void start() {
        if (mLocationThread == null || !mLocationThread.isAlive()) {
            mLocationThread = new LocationThread(mContext.getApplicationContext(), this);
            mLocationThread.startThread();
        }
        showJoyStick();
        mIsStarted = true;
    }

    public void stop() {
        if (mLocationThread != null) {
            mLocationThread.stopThread();
            mLocationThread = null;
        }

        hideJoyStick();
        mIsStarted = false;
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void showJoyStick() {
        Intent intent = new Intent(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 01, intent, Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext.getApplicationContext());
        builder.setContentTitle(mContext.getText(R.string.app_name));
//        builder.setContentText("This is the text");
//        builder.setSubText("Some sub text");
        builder.setNumber(101);
        builder.setContentIntent(pendingIntent);
//        builder.setTicker("Fancy Notification");
        builder.setSmallIcon(R.drawable.icon_app);
        //builder.setLargeIcon(bm);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        Notification notification = builder.build();
        NotificationManager notificationManger =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.notify(01, notification);

    }

    public void hideJoyStick() {
        NotificationManager notificationManger =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.cancel(01);
    }

    public LocPoint getCurrentLocPoint() {
        return mCurrentLocPoint;
    }

    public LocPoint getUpdateLocPoint() {
        if (!mIsFlyMode || mFlyTimeIndex > mFlyTime) {
            return mCurrentLocPoint;
        } else {
            float factor = (float) mFlyTimeIndex / (float) mFlyTime;
            double lat = mCurrentLocPoint.getLatitude() + (factor * (mTargetLocPoint.getLatitude() - mCurrentLocPoint.getLatitude()));
            double lon = mCurrentLocPoint.getLongitude() + (factor * (mTargetLocPoint.getLatitude() - mCurrentLocPoint.getLatitude()));
            mCurrentLocPoint.setLatitude(lat);
            mCurrentLocPoint.setLongitude(lon);
            mFlyTimeIndex++;
            return mCurrentLocPoint;
        }
    }

    public void jumpToLocation(@NonNull LocPoint location) {
        mIsFlyMode = false;
        mCurrentLocPoint = location;
    }

    public void flyToLocation(@NonNull LocPoint location, int flyTime) {
        mTargetLocPoint = location;
        mFlyTime = flyTime;
        mFlyTimeIndex = 0;
        mIsFlyMode = true;
    }

    public boolean isFlyMode() {
        return mIsFlyMode;
    }

    public void stopFlyMode() {
        mIsFlyMode = false;
    }

    public void setMoveStep(double moveStep) {
        mMoveStep = moveStep;
    }

    public double getMoveStep() {
        return mMoveStep;
    }


    @Override
    public void onSetLocationClick() {
        Logger.d(TAG, "onSetLocationClick");
        MainActivity.startPage(mContext);
    }

    @Override
    public void onBookmarkLocationClick() {
        Logger.d(TAG, "onBookmarkLocationClick");
        if (mCurrentLocPoint != null) {
            LocPoint locPoint = new LocPoint(mCurrentLocPoint);
            BookmarkActivity.startPage(mContext, "Bookmark", locPoint);
            Toast.makeText(mContext, "Current location is copied!" + "\n" + locPoint, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Service is not start!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyLocationClick() {
        Logger.d(TAG, "onCopyLocationClick");
        if (mCurrentLocPoint != null) {
            FakeGpsUtils.copyToClipboard(mContext, mCurrentLocPoint.toString());
            Toast.makeText(mContext, "Current location is copied!" + "\n" + mCurrentLocPoint, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onArrowUpClick() {
        Logger.d(TAG, "onArrowUpClick");
        mCurrentLocPoint.setLatitude(mCurrentLocPoint.getLatitude() + mMoveStep);
    }

    @Override
    public void onArrowDownClick() {
        Logger.d(TAG, "onArrowDownClick");
        mCurrentLocPoint.setLatitude(mCurrentLocPoint.getLatitude() - mMoveStep);
    }

    @Override
    public void onArrowLeftClick() {
        Logger.d(TAG, "onArrowLeftClick");
        mCurrentLocPoint.setLongitude(mCurrentLocPoint.getLongitude() - mMoveStep);
    }

    @Override
    public void onArrowRightClick() {
        Logger.d(TAG, "onArrowRightClick");
        mCurrentLocPoint.setLongitude(mCurrentLocPoint.getLongitude() + mMoveStep);
    }

}
