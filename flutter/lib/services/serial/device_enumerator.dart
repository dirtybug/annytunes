import 'package:usb_serial/usb_serial.dart';

import 'usb_serial_port.dart';

/// Discovers attached USB serial devices and opens [UsbSerialPort] instances.
class DeviceEnumerator {
  /// Return all currently attached USB serial devices.
  Future<List<UsbDevice>> listDevices() async {
    return UsbSerial.listDevices();
  }

  /// Create and open a [UsbSerialPort] for the given [device].
  Future<UsbSerialPort> openDevice(UsbDevice device) async {
    final port = UsbSerialPort(device);
    await port.open();
    return port;
  }
}
