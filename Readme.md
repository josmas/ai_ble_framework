# Messy BLE

A really messy BLE implementation to experiment with BLE in order to write
a little framework for BLE support in App Inventor.

So far it has a main activity, which is an abstract Base class (abstract non visible Component
when translated into App Inventor), hosting its own broadcast receiver, that handles messages
from a Service. Most of the code has been adapted from
https://github.com/googlesamples/android-BluetoothLeGatt

The idea for the component is to have an abstract base class that deals with the scanning
and handling broadcasts from the service, and that can be extended to parse particular data from
a BLE powered device, such as a heart rate monitor or a temperature sensor. The framework takes
care of everything but the particular Characteristics and Services of the device. The component
for now operates in Central mode (the device will be peripheral) and acts as a BLE client (the
device will be the server).

Two Activities extend the main abstact class right now, one to connect to a Heart Rate monitor,
and one to connect to an RFDuino powered device (containing accelerometer and gyroscope).
The service takes care of the GATT connection and of distributing/broadcasting data, while the
activities take care of translating the data coming from the Service into human readable data.
Right now the activities unregister the receiver onPause, so no data circulates when the activity
is not visible. A different way of circulating data through the service would be a good
enhancement (thinking about how to do that).

Jos - March 2o16
