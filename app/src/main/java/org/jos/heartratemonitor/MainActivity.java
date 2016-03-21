package org.jos.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final int REQUEST_ENABLE_BT = 1;
  private static final long SCAN_PERIOD = 20000;
  private BluetoothAdapter mBluetoothAdapter;
  private List mLeDevices = new ArrayList<BluetoothDevice>();
  private boolean isScanning;
  private boolean bleSupported = false;
  private Button connectButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });

    connectButton = (Button) findViewById(R.id.connectButton);
    connectButton.setEnabled(false);

    if (bleSupport()){
      ((TextView)findViewById(R.id.state)).setText(R.string.ble_available);
      bleEnabled();
      if (mBluetoothAdapter.isEnabled()){
        bleSupported = true;
      }
    }
    else {
      ((TextView)findViewById(R.id.state)).setText(R.string.ble_not_supported);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (bleSupported) {
      ((TextView)findViewById(R.id.state)).setText("Starting scanner");
      startScanning();
    }
    else {
      ((TextView)findViewById(R.id.state)).setText("BLE does not seem to be available");
    }
  }

  private boolean bleSupport() {
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
      return false;
    }
    return true;
  }

  private void bleEnabled() {
    // Initializes Bluetooth adapter.
    final BluetoothManager bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      mBluetoothAdapter = bluetoothManager.getAdapter();
      // Ensures Bluetooth is available on the device and it is enabled. If not,
      // displays a dialog requesting user permission to enable Bluetooth.
      if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
    else {
      ((TextView)findViewById(R.id.state)).setText(R.string.ble_not_supported);
    }
  }

  public void startScanning() {
    Handler mHandler = new Handler();
    if (!mLeDevices.isEmpty()) {
      mLeDevices.clear();
    }
    // Stops scanning after a pre-defined scan period.
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        isScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        ((TextView)findViewById(R.id.state)).setText("Stopped Scanning");
      }
    }, SCAN_PERIOD);
    mBluetoothAdapter.startLeScan(mLeScanCallback);
  }

  public void stopScanning() {
    mBluetoothAdapter.stopLeScan(mLeScanCallback);
  }

  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          isScanning = true;
          addDevice(device, rssi, scanRecord);
          Log.i("BLE", "Got a result: " + device.getName() + " " + rssi + " " + scanRecord.toString());
        }
      });
    }
  };

  // RSSI is the strength of the signal; can be used to approximate distance
  // (not accurate on Android). I won't be using it for now
  private void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (!mLeDevices.contains(device)) {
      mLeDevices.add(device);
      // Running on UIThread so this is fine here
      connectButton.setEnabled(true);
      connectButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          BluetoothDevice first = (BluetoothDevice) mLeDevices.get(0);
          connectToGattServer(first);
        }
      });
    }
  }

  //TODO (jos) Do I need to figure out if the BLE changes state?
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_CANCELED) {
      Toast.makeText(this, "BLE is not cool: " + mBluetoothAdapter.isEnabled(), Toast.LENGTH_LONG).show();
      return;
    }
    else if (resultCode == RESULT_OK){
      Toast.makeText(this, "BLE Active, you can proceed; need to use this properly: " + mBluetoothAdapter.isEnabled(), Toast.LENGTH_LONG).show();
      return;
    }
  }


  // Scanning code was ^^^^^
  // Connection code is from now on
  private void connectToGattServer(final BluetoothDevice device){
    Log.i("BLE", "Connecting with name: " + device.getName());
    Log.i("BLE", "Connecting with address: " + device.getAddress());
    // Running on UIThread so this is fine here
    ((TextView)findViewById(R.id.state)).setText("Connecting to " + device.getName() +
        " at address: " + device.getAddress());
  }
  // Connection code was ^^^^^

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
