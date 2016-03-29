package org.jos.heartratemonitor;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
  private final static String TAG = BluetoothLeService.class.getSimpleName();

  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private String mBluetoothDeviceAddress;
  private BluetoothGatt mBluetoothGatt;
  private int mConnectionState = STATE_DISCONNECTED;

  private static final int STATE_DISCONNECTED = 0;
  private static final int STATE_CONNECTING = 1;
  private static final int STATE_CONNECTED = 2;

  public final static String ACTION_GATT_CONNECTED =
      "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
  public final static String ACTION_GATT_DISCONNECTED =
      "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
  public final static String ACTION_GATT_SERVICES_DISCOVERED =
      "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
  public final static String ACTION_DATA_AVAILABLE =
      "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
  public final static String EXTRA_DATA =
      "com.example.bluetooth.le.EXTRA_DATA";

  public final static UUID UUID_HEART_RATE_MEASUREMENT =
      UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

  // Implements callback methods for GATT events that the app cares about.  For example,
  // connection change and services discovered.
  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String intentAction;
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        intentAction = ACTION_GATT_CONNECTED;
        mConnectionState = STATE_CONNECTED;
        broadcastUpdate(intentAction);
        Log.i(TAG, "Connected to GATT server.");
        // Attempts to discover services after successful connection.
        Log.i(TAG, "Attempting to start service discovery:" +
            mBluetoothGatt.discoverServices());

      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        intentAction = ACTION_GATT_DISCONNECTED;
        mConnectionState = STATE_DISCONNECTED;
        Log.i(TAG, "Disconnected from GATT server.");
        broadcastUpdate(intentAction);
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
      } else {
        Log.w(TAG, "onServicesDiscovered received: " + status);
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
      broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    }
  };

  /**
   * This is the broadcast used for connection changes and discovered services. It is generic so it
   * stays in this class.
   * @param action
   */
  private void broadcastUpdate(final String action) {
    final Intent intent = new Intent(action);
    sendBroadcast(intent);
  }

  /**
   * This is the specific broadcast for a Read or Changed Characteristic and it is specific of each
   * device we are connecting to. It should be extracted to a subclass of this mService.
   * TODO (jos) I have tried extending, but the lifecycle is getting in the way. Going to try and
   * read the data in the Display class instead of in here; working with the RF sensor to try that.
   * @param action
   * @param characteristic
   */
  private void broadcastUpdate(final String action,
                               final BluetoothGattCharacteristic characteristic) {
    final Intent intent = new Intent(action);

    //TODO (jos) I should be able to send everything as a byte array and do the formatting specific
    // for heart rate measurements in the Activity. That way, this Service could be Generic, as long
    // as the code for Notifications (below) can also be made generic.

    // This is special handling for the Heart Rate Measurement profile.  Data parsing is
    // carried out as per profile specifications:
    // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
    if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
      int flag = characteristic.getProperties();
      int format = -1;
      if ((flag & 0x01) != 0) {
        format = BluetoothGattCharacteristic.FORMAT_UINT16;
        Log.d(TAG, "Heart rate format UINT16.");
      } else {
        format = BluetoothGattCharacteristic.FORMAT_UINT8;
        Log.d(TAG, "Heart rate format UINT8.");
      }
      final int heartRate = characteristic.getIntValue(format, 1);
      Log.d(TAG, String.format("Received heart rate: %d", heartRate));
      intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
    } else {
      // For all other profiles, pass on the data as a byte array.
      final byte[] data = characteristic.getValue();
      if (data != null && data.length > 0) {
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
      }
    }
    sendBroadcast(intent);
  }

  public class LocalBinder extends Binder {
    BluetoothLeService getService() {
      return BluetoothLeService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    // After using a given device, you should make sure that BluetoothGatt.close() is called
    // such that resources are cleaned up properly.  In this particular example, close() is
    // invoked when the UI is disconnected from the Service.
    close();
    return super.onUnbind(intent);
  }

  private final IBinder mBinder = new LocalBinder();

  /**
   * Initializes a reference to the local Bluetooth adapter.
   *
   * @return Return true if the initialization is successful.
   */
  public boolean initialize() {
    // For API level 18 and above, get a reference to BluetoothAdapter through
    // BluetoothManager.
    if (mBluetoothManager == null) {
      mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
      if (mBluetoothManager == null) {
        Log.e(TAG, "Unable to initialize BluetoothManager.");
        return false;
      }
    }

    mBluetoothAdapter = mBluetoothManager.getAdapter();
    if (mBluetoothAdapter == null) {
      Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
      return false;
    }

    return true;
  }

  /**
   * Connects to the GATT server hosted on the Bluetooth LE device.
   *
   * @param address The device address of the destination device.
   *
   * @return Return true if the connection is initiated successfully. The connection result
   *         is reported asynchronously through the
   *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
   *         callback.
   */
  public boolean connect(final String address) {
    if (mBluetoothAdapter == null || address == null) {
      Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
      return false;
    }

    // Previously connected device.  Try to reconnect.
    if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
        && mBluetoothGatt != null) {
      Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
      if (mBluetoothGatt.connect()) {
        mConnectionState = STATE_CONNECTING;
        return true;
      } else {
        return false;
      }
    }

    final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    if (device == null) {
      Log.w(TAG, "Device not found.  Unable to connect.");
      return false;
    }
    // We want to directly connect to the device, so we are setting the autoConnect
    // parameter to false.
    mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    Log.d(TAG, "Trying to create a new connection.");
    mBluetoothDeviceAddress = address;
    mConnectionState = STATE_CONNECTING;
    return true;
  }

  /**
   * Disconnects an existing connection or cancel a pending connection. The disconnection result
   * is reported asynchronously through the
   * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
   * callback.
   */
  public void disconnect() {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    mBluetoothGatt.disconnect();
  }

  /**
   * After using a given BLE device, the app must call this method to ensure resources are
   * released properly.
   */
  public void close() {
    if (mBluetoothGatt == null) {
      return;
    }
    mBluetoothGatt.close();
    mBluetoothGatt = null;
  }

  /**
   * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
   * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
   * callback.
   *
   * @param characteristic The characteristic to read from.
   */
  public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    mBluetoothGatt.readCharacteristic(characteristic);
  }

  /**
   * Enables or disables notification on a give characteristic.
   *
   * @param characteristic Characteristic to act on.
   * @param enabled If true, enable notification.  False otherwise.
   */
  public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                            boolean enabled) {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    //TODO (jos) Is this code needed for every different device we need to connect?
    //TODO (jos) If I could not move this, I could send the CCCD with the call from the Activity
    // This is specific to Heart Rate Measurement.
    if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
      BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
          UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
      mBluetoothGatt.writeDescriptor(descriptor);
    }
  }

  /**
   * TODO (jos) If this one works, it generalises the previous Notification method, so delete it if
   * it works after trying it with the Heart Rate monitor.
   * @param characteristic
   * @param enabled
   * @param TARGET_UUID
   * @param CCCD
   */
  public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                            boolean enabled, UUID TARGET_UUID, UUID CCCD) {
    Log.i("BLE", "New method for set notification - YES I AM USING IT with: " + enabled);
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    // CCCD is needed to set up notifications on the device side.
    Log.i("BLE", "UUID_T: " + TARGET_UUID + " and \nUUID_C: " + characteristic.getUuid());
    if (TARGET_UUID.equals(characteristic.getUuid().toString())) { //TODO (jos) is this needed? probably for some devices
      Log.i("BLE", "I am setting the Notification now: " + CCCD);
      BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
      mBluetoothGatt.writeDescriptor(descriptor);
    }
  }

  /**
   * Retrieves a list of supported GATT services on the connected device. This should be
   * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
   *
   * @return A {@code List} of supported services.
   */
  public List<BluetoothGattService> getSupportedGattServices() {
    if (mBluetoothGatt == null) return null;

    return mBluetoothGatt.getServices();
  }
}
