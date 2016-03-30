package org.jos.heartratemonitor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Class that extends from the MainActivity in order to define what to do with Services and
 * Characteristics. MainActivity is Abstract and we use displayServices as a Template method.
 */
public class DisplayHeartRateActivity extends MainActivity {

  public final static String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
  public final static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
  public final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
  public final static String BATTERY_LEVEL_SER = "0000180f-0000-1000-8000-00805f9b34fb";
  public final static String BATTERY_LEVEL_CHAR = "00002a19-0000-1000-8000-00805f9b34fb";
  public final static String CLIENT_CHARACTERISTIC_CONFIG_BATTERY = "00002908-0000-1000-8000-00805f9b34fb";
  private static HashMap<String, String> attributes = new HashMap<>();

  static {
    // Sample Services.
    attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");
    attributes.put(BATTERY_LEVEL_SER, "Battery Level Service");
    // Sample Characteristics.
    attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
    attributes.put(BATTERY_LEVEL_CHAR, "Battery Level Measurement");
  }

  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }

  private BluetoothGattCharacteristic mNotifyCharacteristic;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  /**
   * Discovering Services and Characteristics. For now, setting up Notification for hear rate, but
   * the battery service could also be used. The monitor has also additional services.
   * @param supportedGattServices
   */
  @Override
  //TODO (jos) It is more of a setupServicesAndCharacteristics than a displayServices
  public void displayServices(List<BluetoothGattService> supportedGattServices) {
    Log.i("BLE", "All Services are: " + supportedGattServices);

    if (supportedGattServices == null) return;
    String uuid;
    String unknownServiceString = getResources().getString(R.string.unknown_service);
    String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

    for (BluetoothGattService gattService : supportedGattServices) {
      uuid = gattService.getUuid().toString();
      String serviceFound = lookup(uuid, unknownServiceString);
      if (serviceFound.equals(unknownServiceString)) continue;
      Log.i("BLE", "\n\n-------------------------------------------------------------");
      Log.i("BLE", "Service Data for Heart Rate: " + serviceFound);

      // Loops through available Characteristics.
      for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
        uuid = gattCharacteristic.getUuid().toString();
        String characteristicFound = lookup(uuid, unknownCharaString);
        if (characteristicFound.equals(unknownCharaString)) continue; // Skip if not interested.
        Log.i("BLE", "Characteristic for Heart Rate monitor: " + characteristicFound);

        // Tried to read the value from gattCharacteristic here, but seems like it's null at this
        // stage. It needs to be read in the service (on callback).
        // Read and Notify for the heart rate measure; this should be its own method.
        //TODO (jos) for now just setting a Notification in the Heart Rate Meassure.
        // Could also provide a method to read from the battery level characteristic.
        if (UUID.fromString(HEART_RATE_MEASUREMENT).equals(gattCharacteristic.getUuid())) {

          final int charaProp = gattCharacteristic.getProperties();
          if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
              bluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false,
                  gattCharacteristic.getUuid(), UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
              mNotifyCharacteristic = null;
            }
            bluetoothLeService.readCharacteristic(gattCharacteristic);
          }
          if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true,
                gattCharacteristic.getUuid(), UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
          }
        }
      }
    }

  }

  @Override
  public void displayData(String data) {
    Log.i("BLE", "We got data: " + data);
  }

  @Override
  protected void displayData(byte[] data) {
    Log.i("BLE", "We got data: TESTING THIS STUFF - DELETE THE PREVIOUS ONE OF THIS WORKS " + data);
  }
}
