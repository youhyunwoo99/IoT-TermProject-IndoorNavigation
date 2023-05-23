package com.navigation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerDistanceCalculator implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float[] gravity = new float[3]; // Gravity components
    private float[] linearAcceleration = new float[3]; // Linear acceleration components

    private float[] currentVelocity = new float[3]; // Current velocity along each axis
    private float[] currentPosition = new float[3]; // Current position along each axis
    private float[] lastPosition = new float[3]; // Last known position
    private float totalDistance = 0;

    private long lastTime;
    private OnDistanceChangedListener listener;
    public interface OnDistanceChangedListener {
        void onDistanceChanged(double distance);
    }
    public AccelerometerDistanceCalculator(SensorManager sensorManager, OnDistanceChangedListener listener) {
        this.sensorManager = sensorManager;
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        lastTime = System.currentTimeMillis();
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get current time in milliseconds
        long now = System.currentTimeMillis();

        // Calculate time elapsed since last reading
        float dt = (now - lastTime) / 1000.0f; // in seconds

        // Isolate the force of gravity with the low-pass filter.
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linearAcceleration[0] = event.values[0] - gravity[0];
        linearAcceleration[1] = event.values[1] - gravity[1];
        linearAcceleration[2] = event.values[2] - gravity[2];

        // Calculate velocity and position
        for (int i = 0; i < 3; i++) {
            currentVelocity[i] += (linearAcceleration[i] * dt);
            currentPosition[i] += currentVelocity[i] * dt;
        }

        // Calculate distance travelled
        float dx = currentPosition[0] - lastPosition[0];
        float dy = currentPosition[1] - lastPosition[1];
        float dz = currentPosition[2] - lastPosition[2];
        float distanceChange = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        totalDistance += distanceChange;

        // Update last position
        System.arraycopy(currentPosition, 0, lastPosition, 0, 3);

        // Call listener
        if (listener != null) {
            listener.onDistanceChanged(totalDistance);
        }

        // Update last time
        lastTime = now;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    public void reset() {
        for (int i = 0; i < 3; i++) {
            gravity[i] = 0f;
            linearAcceleration[i] = 0f;
            currentVelocity[i] = 0f;
            currentPosition[i] = 0f;
            lastPosition[i] = 0f;
        }
        totalDistance = 0;
    }
}