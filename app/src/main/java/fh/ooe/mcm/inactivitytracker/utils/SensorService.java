package fh.ooe.mcm.inactivitytracker.utils;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;

import java.util.ArrayList;

import fh.ooe.mcm.inactivitytracker.interfaces.Observable;
import fh.ooe.mcm.inactivitytracker.interfaces.Observer;

public class SensorService extends Service implements SensorEventListener, Observable, Observer {

    int READING_RATE = 50000;

    private SensorManager sensorManager;
    private Sensor sensor;

    HandlerThread sensorThread;
    Handler sensorHandler;

    PowerManager.WakeLock wakeLock;

    ArrayList<Observer> observers;

    public SensorService(Observer observer, SensorManager sensorManager, PowerManager powerManager) {
        observers = new ArrayList<>();
        observers.add(observer);

        this.sensorManager = sensorManager;
        this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorThread = new HandlerThread("Sensor thread", Process.THREAD_PRIORITY_BACKGROUND);
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());
        sensorManager.registerListener(this, sensor, READING_RATE, sensorHandler);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myApp:myWakeTag");
        wakeLock.acquire();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            double timestamp = System.currentTimeMillis();

            notifyAll(new double [] {x, y, z, timestamp});
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing here.
    }

    //@androidx.annotation.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void notifyAll(Object object) {
        for(Observer observer: observers) {
            observer.update(this, object);
        }
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void update(Observable observable, Object object) {
        if(observable instanceof Recognizer) {
            if(object instanceof Boolean) {
                Boolean shouldBeSensing = (Boolean) object;
                if(shouldBeSensing) {
                    sensorManager.unregisterListener(this, sensor);
                } else {
                    sensorManager.registerListener(this, sensor, READING_RATE, sensorHandler);
                }
            }
        }
    }
}
