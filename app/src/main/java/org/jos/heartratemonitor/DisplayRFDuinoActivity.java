package org.jos.heartratemonitor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Class that extends from the MainActivity in order to define what to do with Services and
 * Characteristics. MainActivity is Abstract and we use displayServices as a Template method.
 */
public class DisplayRFDuinoActivity extends MainActivity {

  private BluetoothGattCharacteristic mNotifyCharacteristic;

  // Services and Characteristics we are interested in
  public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";
  public final static UUID UUID_SERVICE = sixteenBitUuid(0x2220);
  public final static UUID UUID_RECEIVE = sixteenBitUuid(0x2221);
  public final static UUID UUID_CLIENT_CONFIGURATION = sixteenBitUuid(0x2902);

  public static UUID sixteenBitUuid(long shortUuid) {
    if(!(shortUuid >= 0 && shortUuid <= 0xFFFF)) throw new RuntimeException(); //TODO (jos) reconsider.
    return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  /**
   * In this method you should loop till you find the services you want, and within those, you can
   * find the Characteristics you want. After that, you read or set a notification for them.
   * @param supportedGattServices
   */
  @Override
  //TODO (jos) It is more of a setupServicesandCharas than a displayServices
  public void displayServices(List<BluetoothGattService> supportedGattServices) {
    Log.i("BLE", "All Services are: " + supportedGattServices);

    if (supportedGattServices == null) return;
    String uuid;

    for (BluetoothGattService gattService : supportedGattServices) {
      uuid = gattService.getUuid().toString();
      if (!uuid.equals(UUID_SERVICE.toString())) continue; // Skip if not interested
      Log.i("BLE", "\n\n-------------------------------------------------------------");
      Log.i("BLE", "Service Data for RFDuino: " + gattService.getUuid());
      // Instead of looping, let's just grab it!
      BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID_RECEIVE);
      if (gattCharacteristic != null) {
        // Tried to read the value from gattCharacteristic here, but seems like it's null at this
        // stage. It needs to be read in the service (on callback).
        // Read and Notify for the heart rate measure; this should be its own method.
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
          // If there is an active notification on a characteristic, clear
          // it first so it doesn't update the data field on the user interface.
          if (mNotifyCharacteristic != null) {
            //TODO (jos) using the new method in order to make the service Generic. Have to test
            // with Heart Rate monitor.
            bluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false,
                UUID_RECEIVE, UUID_CLIENT_CONFIGURATION);
            mNotifyCharacteristic = null;
          }
          bluetoothLeService.readCharacteristic(gattCharacteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
          mNotifyCharacteristic = gattCharacteristic;
          bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true,
              UUID_RECEIVE, UUID_CLIENT_CONFIGURATION);
        }
      } else {
        Log.e("BLE", "RFduino received characteristic not found!");
      }
    }
  }

  @Override
  public void displayData(byte[] data) {
    if (data != null ) {
      Log.i("BLE", "GyroData (6 bytes): " + "\nX: " + gyroX(data) + "\nY: " + gyroY(data) + "\nZ: " + gyroZ(data));

      // Accelerometer data
      float heading = filterYaw.lowPass(accelX(data));
      float pitch = filterPitch.lowPass(accelY(data));
      float roll = filterRoll.lowPass(accelZ(data));

      Log.i("BLE", "accelData (6 bytes): "   + "\nX: " + heading
          + "\nY: " + pitch
          + "\nZ: " + roll);
    }
  }

  // The following code comes from Darren McNeely's VRA app.
  static float accelRes = (8.0f / 32768.0f) ;	// scale resolutions for the MPU6050 (scale set to ±8g, 16bit sample)
  static float gyro_sensitivity = 131; // Convert to deg/s

  public static float gyroX(byte data[]){
    return (((data[12] << 8) + (data[13] & 0xFF))/gyro_sensitivity) + 2; // +2 to initialise as 0
  }
  public static float gyroY(byte data[]){
    return (((data[14] << 8) + (data[15] & 0xFF))/gyro_sensitivity);
  }
  public static float gyroZ(byte data[]){
    return (((data[16] << 8) + (data[17] & 0xFF))/gyro_sensitivity);
  }

  public static float accelX(byte data[]){
    return ((data[5] << 8) + (data[6] & 0xFF))* accelRes;
  }
  public static float accelY(byte data[]){
    return ((data[7] << 8) + (data[8] & 0xFF)) * accelRes;
  }
  public static float accelZ(byte data[]){
    return ((data[9] << 8) + (data[10] & 0xFF)) * accelRes;
  }

  class LowPassFilter {
    /*
     * time smoothing constant for low-pass filter 0 ? alpha ? 1 ; a smaller
     * /Low-pass_filter#Discrete-time_realization
     */
    float ALPHA = 0f;
    float lastOutput = 0;

    public LowPassFilter(float ALPHA) {
      this.ALPHA = ALPHA;
    }

    public float lowPass(float input) {
      if (Math.abs(input - lastOutput) > 170) {
        lastOutput = input;
        return lastOutput;
      }
      lastOutput = lastOutput + ALPHA * (input - lastOutput);
      return lastOutput;
    }
  }

  LowPassFilter filterYaw = new LowPassFilter(0.03f);
  LowPassFilter filterPitch = new LowPassFilter(0.03f);
  LowPassFilter filterRoll = new LowPassFilter(0.03f);

}
