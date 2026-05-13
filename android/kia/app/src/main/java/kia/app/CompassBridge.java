package kia.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;

final class CompassBridge implements SensorEventListener, LocationListener {
    private static final int BUS_M_CAN = 1;
    private static final byte[] NAV_STATUS_114 = {
            0x0B, 0x21, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xE1, 0x0F
    };
    private static final byte[] NAV_STATUS_197 = {
            0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static CompassBridge instance;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float[] accel = new float[3];
    private final float[] magnetic = new float[3];
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            sendCompass();
            handler.postDelayed(this, AppPrefs.navCompass(context) ? 700L : 2000L);
        }
    };

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private boolean accelReady;
    private boolean magneticReady;
    private float headingDegrees;
    private long lastHeadingAt;
    private long lastStatusAt;
    private long lastLogAt;
    private String headingSource = "neutral";

    private CompassBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    static synchronized void start(Context context) {
        if (context == null) return;
        if (instance == null) instance = new CompassBridge(context);
        instance.startInternal();
    }

    static synchronized void stop() {
        if (instance == null) return;
        instance.stopInternal();
        instance = null;
    }

    static synchronized void refresh(Context context) {
        if (context == null) return;
        if (AppPrefs.navCompass(context)) start(context);
        else if (instance != null) instance.stopInternal();
    }

    private void startInternal() {
        registerSensors();
        registerLocation();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void stopInternal() {
        handler.removeCallbacks(tick);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }
        }
    }

    private void registerSensors() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;
        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation != null) {
            sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
            return;
        }
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (magneticSensor != null) {
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void registerLocation() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;
        try {
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) onLocationChanged(last);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotation = new float[9];
            float[] orientation = new float[3];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
            setHeading((float) Math.toDegrees(orientation[0]), "sensor");
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accel, 0, Math.min(event.values.length, accel.length));
            accelReady = true;
            updateFallbackOrientation();
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetic, 0, Math.min(event.values.length, magnetic.length));
            magneticReady = true;
            updateFallbackOrientation();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && location.hasBearing() && location.getSpeed() > 0.8f) {
            setHeading(location.getBearing(), "gps");
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void updateFallbackOrientation() {
        if (!accelReady || !magneticReady) return;
        float[] rotation = new float[9];
        float[] inclination = new float[9];
        if (!SensorManager.getRotationMatrix(rotation, inclination, accel, magnetic)) return;
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotation, orientation);
        setHeading((float) Math.toDegrees(orientation[0]), "mag");
    }

    private void setHeading(float degrees, String source) {
        headingDegrees = normalize(degrees);
        headingSource = source;
        lastHeadingAt = System.currentTimeMillis();
    }

    private void sendCompass() {
        if (!AppPrefs.navCompass(context)) return;
        long now = System.currentTimeMillis();
        if (now - lastStatusAt > 1800L) {
            lastStatusAt = now;
            CanbusControl.sendRawCanQuiet(context, BUS_M_CAN, 0x114, NAV_STATUS_114);
            CanbusControl.sendRawCanQuiet(context, BUS_M_CAN, 0x197, NAV_STATUS_197);
        }
        byte[] frame = new byte[8];
        int raw = Math.round((currentHeading(now) + 7.5f) / 7.5f) & 0x3F;
        frame[5] = (byte) raw;
        CanbusControl.sendRawCanQuiet(context, BUS_M_CAN, 0x1E6, frame);
        if (AppPrefs.debug(context) && now - lastLogAt > 5000L) {
            lastLogAt = now;
            AppLog.line(context, String.format(Locale.US,
                    "Компас: %.0f° raw=%02X source=%s", currentHeading(now), raw, headingSource));
        }
    }

    private float currentHeading(long now) {
        if (lastHeadingAt == 0 || now - lastHeadingAt > 20000L) return headingDegrees;
        return headingDegrees;
    }

    private static float normalize(float value) {
        float out = value % 360f;
        if (out < 0f) out += 360f;
        return out;
    }
}
