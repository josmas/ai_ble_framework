package org.jos.heartratemonitor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * This was an Experiment; It is not being used right now!
 * DELETE If the second Broadcast Receiver Idea works out. Go back to this idea if it does not!
 */
public class HeartRateService extends BluetoothLeService {

  private final static String TAG = "HEART_RATE";

  @Override
  public IBinder onBind(Intent intent) {
    return super.onBind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    return super.onUnbind(intent);
  }

  /**
   * This is the specific broadcast for a Read or Changed Characteristic and it is specific of each
   * device we are connecting to. It should be extracted to a subclass of this mService.
   * @param action
   * @param characteristic
   */
//  @Override
  protected void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

    Log.d(TAG, "OH MY!!!! I am working from the subclass of the service. I am THE AWESOME!");
    final Intent intent = new Intent(action);

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
      // For all other profiles, writes the data formatted in HEX.
      final byte[] data = characteristic.getValue();
      if (data != null && data.length > 0) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for(byte byteChar : data)
          stringBuilder.append(String.format("%02X ", byteChar));
        intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
      }
    }
    sendBroadcast(intent);
  }
}
