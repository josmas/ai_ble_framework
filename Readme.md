# Messy BLE

A really messy BLE implementation to experiment with BLE in ofer to write
a little framework for BLE support in App Inventor.

So far it has a main activity (which will be a Base class non visible
Component), hosting its own broadcast receiver, that handles messages from a
Service. Most of the code has been adapted from
https://github.com/googlesamples/android-BluetoothLeGatt

The idea for the component is to have a base class that deals with the scanning
and handling broadcasts from the service, and that can be extended to parse
particular data from a BLE powered device, such as a heart rate monitor or a
temperature sensor. The framework will take care of everything but the
particular characteristics and services of the device. The component will
operate in Central mode (the device will be peripheral) and act as a BLE
client (the device will be the server).

Jos - March 2o16
