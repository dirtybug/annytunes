import 'dart:typed_data';

/// Abstract interface for serial port communication.
///
/// Implementations may wrap USB serial hardware, Bluetooth SPP,
/// or a mock for testing.
abstract class ISerialPort {
  Future<void> open();
  Future<void> close();
  Future<void> write(Uint8List data);
  Stream<Uint8List> get readStream;
  bool get isOpen;
}
