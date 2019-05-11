package com.github.fakegps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.ILocationManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.github.fakegps.model.LocPoint;
import com.github.fakegps.ui.MainActivity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.taimos.gpsd4java.api.ObjectListener;
import de.taimos.gpsd4java.backend.GPSdEndpoint;
import de.taimos.gpsd4java.backend.ResultParser;
import de.taimos.gpsd4java.types.ATTObject;
import de.taimos.gpsd4java.types.DeviceObject;
import de.taimos.gpsd4java.types.DevicesObject;
import de.taimos.gpsd4java.types.ENMEAMode;
import de.taimos.gpsd4java.types.SATObject;
import de.taimos.gpsd4java.types.SKYObject;
import de.taimos.gpsd4java.types.TPVObject;
import de.taimos.gpsd4java.types.subframes.SUBFRAMEObject;
import tiger.radio.loggerlibrary.Logger;

/**
 * LocationThread
 * Created by tiger on 7/21/16.
 * Switched to GPSd by Sven@Killig.de on 2019/05/11
 */
public class LocationThread extends HandlerThread {

    private static final String TAG = "LocationThread";


    private Context mContext;
    private JoyStickManager mJoyStickManager;
    private LocationManager mLocationManager;

    private Handler mHandler;
    private LocPoint mLastLocPoint = new LocPoint(0, 0);

    private static Method mMethodMakeComplete;
    private static ILocationManager mILocationManager;

    GPSdEndpoint ep;
    SharedPreferences sharedPref;

    public LocationThread(Context context, JoyStickManager joyStickManager) {
        super("LocationThread");
        mContext = context;
        mJoyStickManager = joyStickManager;

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mMethodMakeComplete == null) {
            try {
                mMethodMakeComplete = Location.class.getMethod("makeComplete", new Class[0]);
            } catch (NoSuchMethodException e) {
                Logger.e(TAG, "get Location.makeComplete method fail!", e);
            }
        }

        if (mILocationManager == null) {
            Field declaredField = null;
            try {
                declaredField = Class.forName(mLocationManager.getClass().getName()).getDeclaredField("mService");
                declaredField.setAccessible(true);
                mILocationManager = (ILocationManager) declaredField.get(mLocationManager);
            } catch (Exception e) {
                Logger.e(TAG, "get LocationManager mService fail!", e);
            }
        }


        try {
            sharedPref= PreferenceManager.getDefaultSharedPreferences(mContext);
            String value = sharedPref.getString(MainActivity.PREFS_KEY, MainActivity.PREFS_DEFAULT);
            final String[] parts = value.split(":");
            ep = new GPSdEndpoint(parts[0], Integer.parseInt(parts[1]), new ResultParser());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void start() {
        super.start();

        mHandler = new Handler(getLooper());
        mHandler.post(mUpdateLocation);
    }

    public void startThread() {
        start();
    }

    public void stopThread() {
        mHandler.removeCallbacksAndMessages(null);
        try {
            quit();
            interrupt();
            ep.stop();
        } catch (Exception e) {
            Logger.e(TAG, "stopThread fail!", e);
        }

        mJoyStickManager = null;
    }


    protected static boolean setMockLocation(int i, Context context) {
        Logger.d(TAG, "setMockLocation " + i);
        try {
            return Settings.Secure.putInt(context.getContentResolver(), "mock_location", i);
        } catch (Exception e) {
            return false;
        }
    }

    Runnable mUpdateLocation = new Runnable() {
        @Override
        public void run() {
            ep.addListener(new ObjectListener() {

                @Override
                public void handleTPV(final TPVObject tpv) {
                    Logger.i(TAG, "TPV: "+tpv);

                    if(tpv.getMode() != ENMEAMode.NotSeen && tpv.getMode() != ENMEAMode.NoFix) {
                        Location location = new Location("gps");
                        location.setLatitude(tpv.getLatitude());
                        location.setLongitude(tpv.getLongitude());
                        location.setAltitude(tpv.getAltitude());
                        if (Build.VERSION.SDK_INT > 16) {
                            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        }
                        location.setAccuracy((float) (tpv.getLatitudeError() + tpv.getLongitudeError()) / 2);
                        location.setSpeed((float) tpv.getSpeed());
                        location.setBearing((float) tpv.getCourse());
                        location.setTime((long) tpv.getTimestamp() * 1000);
                        if (mMethodMakeComplete != null) {
                            try {
                                mMethodMakeComplete.invoke(location, new Object[0]);
                            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            mILocationManager.reportLocation(location, false);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void handleSKY(final SKYObject sky) {
                    Logger.i(TAG, "SKY: "+sky);
                    for (final SATObject sat : sky.getSatellites()) {
//                        Logger.i(TAG, "  SAT: "+sat);
                    }
                }

                @Override
                public void handleSUBFRAME(final SUBFRAMEObject subframe) {
                    Logger.i(TAG, "SUBFRAME: "+subframe);
                }

                @Override
                public void handleATT(final ATTObject att) {
                    Logger.i(TAG, "ATT: "+att);
                }

                @Override
                public void handleDevice(final DeviceObject device) {
                    Logger.i(TAG, "Device: "+device);
                }

                @Override
                public void handleDevices(final DevicesObject devices) {
                    for (final DeviceObject d : devices.getDevices()) {
                        Logger.i(TAG, "Device: "+d);
                    }
                }
            });

            ep.start();

            try {
                Logger.i(TAG, "Version: "+ep.version());
                Logger.i(TAG, "Watch: "+ep.watch(true, true));
                Logger.i(TAG, "Poll: "+ep.poll());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    public Handler getHandler() {
        return mHandler;
    }

}
       
