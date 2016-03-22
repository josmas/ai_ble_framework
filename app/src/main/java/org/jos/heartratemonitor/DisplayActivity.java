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
public class DisplayActivity extends MainActivity {

  private final String LIST_NAME = "NAME";
  private final String LIST_UUID = "UUID";
  private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
  private BluetoothGattCharacteristic mNotifyCharacteristic;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

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
      Log.i("BLE", "Service Data is: " + currentServiceData);

      ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
      List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
      ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

      // Loops through available Characteristics.
      for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
        uuid = gattCharacteristic.getUuid().toString();
        String characteristicFound = SampleGattAttributes.lookup(uuid, unknownCharaString);
        if (characteristicFound.equals(unknownCharaString)) continue;
        Log.i("BLE", "Characteristic is: " + gattCharacteristic);
        charas.add(gattCharacteristic);
        HashMap<String, String> currentCharaData = new HashMap<>();
        currentCharaData.put(LIST_NAME, characteristicFound);
        currentCharaData.put(LIST_UUID, uuid);
        gattCharacteristicGroupData.add(currentCharaData);
        Log.i("BLE", "Characteristic Data is: " + currentCharaData);
        // Tried to read the value from gattCharacteristic here, but seems like it's null at this
        // stage. It needs to be read in the service (on callback).
        // Read and Notify for the heart rate measure
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
}
