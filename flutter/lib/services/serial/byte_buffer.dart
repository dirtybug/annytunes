import 'dart:async';
import 'dart:typed_data';

/// Accumulating byte buffer that converts an asynchronous stream of
/// variable-length chunks into exact-count reads with timeout support.
///
/// Usage:
///   1. Pipe incoming serial data through [addBytes].
///   2. Await [readExact] to get exactly N bytes (or a [TimeoutException]).
///
/// Only one [readExact] call may be outstanding at a time.
class ByteBuffer {
  final List<int> _buf = [];
  Completer<void>? _waiter;

  /// Append incoming bytes to the internal buffer.
  void addBytes(Uint8List data) {
    _buf.addAll(data);
    if (_waiter != null && !_waiter!.isCompleted) {
      _waiter!.complete();
    }
  }

  /// Return exactly [count] bytes, blocking until they arrive or [timeout]
  /// elapses.  Throws [TimeoutException] on timeout.
  ///
  /// Only one concurrent call is allowed; a [StateError] is thrown if a
  /// second call is made while the first is still pending.
  Future<Uint8List> readExact(
    int count, {
    Duration timeout = const Duration(seconds: 5),
  }) async {
    if (_waiter != null && !_waiter!.isCompleted) {
      throw StateError('Another readExact is already pending');
    }

    final deadline = DateTime.now().add(timeout);

    while (_buf.length < count) {
      final remaining = deadline.difference(DateTime.now());
      if (remaining.isNegative) {
        throw TimeoutException(
          'readExact: needed $count bytes but only ${_buf.length} arrived',
          timeout,
        );
      }
      _waiter = Completer<void>();
      try {
        await _waiter!.future.timeout(remaining);
      } on TimeoutException {
        throw TimeoutException(
          'readExact: needed $count bytes but only ${_buf.length} arrived',
          timeout,
        );
      }
    }

    final result = Uint8List.fromList(_buf.sublist(0, count));
    _buf.removeRange(0, count);
    return result;
  }

  /// Number of bytes currently buffered.
  int get length => _buf.length;

  /// Discard all buffered data.
  void clear() => _buf.clear();
}
