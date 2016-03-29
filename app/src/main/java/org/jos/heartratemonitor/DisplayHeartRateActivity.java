package org.jos.heartratemonitor;

import android.bluetooth.BluetoothGattCharacteristic;
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
public class DisplayHeartRateActivity extends MainActivity {

  //TODO (jos) move here all the constants from SampleGattAttributes that are needed on this Activity only.
  private final String LIST_NAME = "NAME";
  private final String LIST_UUID = "UUID";
  private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
  private BluetoothGattCharacteristic mNotifyCharacteristic;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  /**
   * TODO (jos) This method could return a list of lists with a Service and its Characteristics in
   * each row. I need the characteristics to either read from or set notifications in the mService.
   * So this method should filter only the Services we want, and add them to a list (of lists).
   * TODO (jos) in fact, this method could be placed in the main class and pass a list of services
   * here as a filter... let's see how it goes after creating the one for RFduino.
   * //TODO (jos) after having created the one for RFDuino, this could go two ways; either make it
   * generic and use a filter, or make it specific and only for the extension/device at hand.
   * @param supportedGattServices
   */
  @Override
  public void displayServices(List<BluetoothGattService> supportedGattServices) {
    Log.i("BLE", "All Services are: " + supportedGattServices);

    if (supportedGattServices == null) return;
    String uuid;
    String unknownServiceString = getResources().getString(R.string.unknown_service);
    String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
    ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
    ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();
    mGattCharacteristics = new ArrayList<>();

    for (BluetoothGattService gattService : supportedGattServices) {
      HashMap<String, String> currentServiceData = new HashMap<>();
      uuid = gattService.getUuid().toString();
      String serviceFound = SampleGattAttributes.lookup(uuid, unknownServiceString);
      if (serviceFound.equals(unknownServiceString)) continue;
      currentServiceData.put(LIST_NAME, serviceFound);
      currentServiceData.put(LIST_UUID, uuid);
      gattServiceData.add(currentServiceData);
      Log.i("BLE", "\n\n-------------------------------------------------------------");
      Log.i("BLE", "Service Data for heart rate: " + currentServiceData);

      ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
      List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
      ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

      // Loops through available Characteristics.
      for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
        uuid = gattCharacteristic.getUuid().toString();
        String characteristicFound = SampleGattAttributes.lookup(uuid, unknownCharaString);
        if (characteristicFound.equals(unknownCharaString)) continue; // Skip if not interested.
        Log.i("BLE", "Characteristic for heart rate: " + gattCharacteristic);
        charas.add(gattCharacteristic);
        HashMap<String, String> currentCharaData = new HashMap<>();
        currentCharaData.put(LIST_NAME, characteristicFound);
        currentCharaData.put(LIST_UUID, uuid);
        gattCharacteristicGroupData.add(currentCharaData);
        Log.i("BLE", "Characteristic Data for heart rate: " + currentCharaData);
        // TODO (jos) In theory, this methods ends here, with a list of lists. The read and notification
        // settings should be done in a separate method.

        // Tried to read the value from gattCharacteristic here, but seems like it's null at this
        // stage. It needs to be read in the service (on callback).
        // Read and Notify for the heart rate measure; this should be its own method.
        if (UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT).equals(gattCharacteristic.getUuid())) {

          final int charaProp = gattCharacteristic.getProperties();
          if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
              bluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
              mNotifyCharacteristic = null;
            }
            bluetoothLeService.readCharacteristic(gattCharacteristic);
          }
          if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
          }
        }
      }
      mGattCharacteristics.add(charas);
      gattCharacteristicData.add(gattCharacteristicGroupData);
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
