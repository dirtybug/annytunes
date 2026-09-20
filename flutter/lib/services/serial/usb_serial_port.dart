import 'dart:typed_data';

import 'package:usb_serial/usb_serial.dart';

import 'serial_port.dart';

/// [ISerialPort] implementation backed by the `usb_serial` Flutter plugin.
///
/// Serial parameters match the Anytone 878UV defaults:
///   115200 baud, 8N1, no flow control.
class UsbSerialPort implements ISerialPort {
  UsbSerialPort(this._device);

  final UsbDevice _device;
  UsbPort? _port;
  bool _open = false;

  @override
  bool get isOpen => _open;

  @override
  Future<void> open() async {
    if (_open) return;

    final port = await _device.create();
    if (port == null) {
      throw StateError('Failed to create serial port for device');
    }
    _port = port;

    final ok = await port.open();
    if (!ok) {
      _port = null;
      throw StateError('Failed to open serial port');
    }

    await port.setDTR(true);
    await port.setRTS(true);
    await port.setPortParameters(
      115200,
      UsbPort.DATABITS_8,
      UsbPort.STOPBITS_1,
      UsbPort.PARITY_NONE,
    );
    await port.setFlowControl(UsbPort.FLOW_CONTROL_OFF);

    _open = true;
  }

  @override
  Future<void> close() async {
    if (!_open) return;
    await _port?.close();
    _port = null;
    _open = false;
  }

  @override
  Future<void> write(Uint8List data) async {
    if (!_open || _port == null) {
      throw StateError('Port is not open');
    }
    await _port!.write(data);
  }

  @override
  Stream<Uint8List> get readStream {
    if (!_open || _port == null) {
      throw StateError('Port is not open');
    }
    return _port!.inputStream!;
  }
}
