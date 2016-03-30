package org.jos.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

public abstract class MainActivity extends AppCompatActivity {

  private static final int REQUEST_ENABLE_BT = 1;
  private static final long SCAN_PERIOD = 20000;
  private BluetoothAdapter mBluetoothAdapter;
  private List mLeDevices = new ArrayList<BluetoothDevice>();
  private boolean isScanning;
  private boolean bleSupported = false;
  private String deviceAddress;
  private Button connectButton;
  private boolean connected = false;
  BluetoothLeService bluetoothLeService;

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
        // Initialise the Service if BLE is available and enabled.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.i("BLE", "Sent Intent to initialise the Service; should run from now on.");
      }
    }
    else {
      ((TextView)findViewById(R.id.state)).setText(R.string.ble_not_supported);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // TODO (jos) the logic here should change. Scan should be a button, and here should be some of
    // the code in the connect button listener.
    if (bleSupported) {
      ((TextView)findViewById(R.id.state)).setText("Starting scanner");
      registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
      startScanning();
    }
    else {
      ((TextView)findViewById(R.id.state)).setText("BLE does not seem to be available");
    }
  }

  @Override
  protected void onPause() {
    try {
      unregisterReceiver(mGattUpdateReceiver);
    } catch (IllegalArgumentException e) {
      // Ignore if the receiver was not registered.
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    unbindService(mServiceConnection);
    bluetoothLeService = null;
    super.onDestroy();
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
      Toast.makeText(this, "BLE Active; all good to go!", Toast.LENGTH_LONG).show();
      return;
    }
  }


  // Scanning code was ^^^^^
  // Connection code is from now on
  private void connectToGattServer(final BluetoothDevice device){
    Log.i("BLE", "Connecting with name: " + device.getName());
    deviceAddress = device.getAddress();
    Log.i("BLE", "Connecting with address: " + deviceAddress);
    // Running on UIThread so this is fine here
    ((TextView)findViewById(R.id.state)).setText("Connecting to " + device.getName() +
        " at address: " + deviceAddress);

    // Actual connection through the Service
    if (bluetoothLeService != null) {
      final boolean result = bluetoothLeService.connect(deviceAddress);
      Log.d("BLE", "Connect request result=" + result);
    }
    else {
      Log.i("BLE", "bluetoothLeService is null and won't connect!");
    }
  }

  // Code to manage Service lifecycle.
  private final ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      Log.i("BLE", "The Service has actually connected.");
      bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
      Log.i("BLE", "The Service has actually connected, and bluetoothLeService is not null anymore.");
      if (!bluetoothLeService.initialize()) {
        Log.e("BLE", "Unable to initialize Bluetooth on the Service; exiting!");
        finish();
      }
      // Automatically connects to the device upon successful start-up initialization.
      if ( deviceAddress != null && !deviceAddress.isEmpty()) {
        bluetoothLeService.connect(deviceAddress);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      Log.i("BLE", "The Service has actually DISconnected, so it is null again.");
      bluetoothLeService = null;
    }
  };

  // A Broadcast Receiver to handle broadcasts from the Service
  // Handles various events fired by the Service.
  // ACTION_GATT_CONNECTED: connected to a GATT server.
  // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
  // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
  // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
  //                        or notification operations.
  private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
        connected = true;
        Log.i("BLE", "Connected to GATT Server");
      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
        connected = false;
        Log.i("BLE", "Connected to GATT Server");
      } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
        displayServices(bluetoothLeService.getSupportedGattServices());
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
          Object data = bundle.get(BluetoothLeService.EXTRA_DATA);
          displayData((byte[]) data);
        }
      }
    }
  };

  public abstract void displayServices(List<BluetoothGattService> supportedGattServices);
  protected abstract void displayData(byte[] data);

  private static IntentFilter makeGattUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
    return intentFilter;
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
