import 'dart:async';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:annytunes/services/serial/byte_buffer.dart';

void main() {
  late ByteBuffer buffer;

  setUp(() {
    buffer = ByteBuffer();
  });

  test('readExact returns immediately when bytes are already buffered', () async {
    buffer.addBytes(Uint8List.fromList([1, 2, 3, 4]));

    final result = await buffer.readExact(4);
    expect(result, equals(Uint8List.fromList([1, 2, 3, 4])));
    expect(buffer.length, 0);
  });

  test('readExact returns partial prefix and leaves remainder', () async {
    buffer.addBytes(Uint8List.fromList([10, 20, 30, 40, 50]));

    final result = await buffer.readExact(3);
    expect(result, equals(Uint8List.fromList([10, 20, 30])));
    expect(buffer.length, 2);
  });

  test('readExact waits for data added later', () async {
    final future = buffer.readExact(3, timeout: const Duration(seconds: 2));

    // Add bytes after a short delay.
    await Future<void>.delayed(const Duration(milliseconds: 50));
    buffer.addBytes(Uint8List.fromList([7, 8, 9]));

    final result = await future;
    expect(result, equals(Uint8List.fromList([7, 8, 9])));
  });

  test('readExact accumulates multiple chunks', () async {
    final future = buffer.readExact(5, timeout: const Duration(seconds: 2));

    await Future<void>.delayed(const Duration(milliseconds: 10));
    buffer.addBytes(Uint8List.fromList([1, 2]));

    await Future<void>.delayed(const Duration(milliseconds: 10));
    buffer.addBytes(Uint8List.fromList([3, 4, 5]));

    final result = await future;
    expect(result, equals(Uint8List.fromList([1, 2, 3, 4, 5])));
  });

  test('readExact throws TimeoutException when not enough bytes arrive', () async {
    buffer.addBytes(Uint8List.fromList([1]));

    expect(
      () => buffer.readExact(5, timeout: const Duration(milliseconds: 100)),
      throwsA(isA<TimeoutException>()),
    );
  });

  test('readExact throws StateError on concurrent calls', () async {
    // Start a pending read.
    final first = buffer.readExact(10, timeout: const Duration(seconds: 2));

    // A second call while the first is pending should fail immediately.
    expect(
      () => buffer.readExact(1, timeout: const Duration(milliseconds: 50)),
      throwsA(isA<StateError>()),
    );

    // Fulfil the first read so the test can complete cleanly.
    buffer.addBytes(Uint8List.fromList(List.filled(10, 0)));
    await first;
  });

  test('clear discards buffered bytes', () {
    buffer.addBytes(Uint8List.fromList([1, 2, 3]));
    expect(buffer.length, 3);
    buffer.clear();
    expect(buffer.length, 0);
  });

  test('readExact with count 0 returns empty list immediately', () async {
    final result = await buffer.readExact(0);
    expect(result, equals(Uint8List(0)));
  });

  test('sequential readExact calls work correctly', () async {
    buffer.addBytes(Uint8List.fromList([1, 2, 3, 4, 5, 6]));

    final first = await buffer.readExact(3);
    expect(first, equals(Uint8List.fromList([1, 2, 3])));

    final second = await buffer.readExact(3);
    expect(second, equals(Uint8List.fromList([4, 5, 6])));

    expect(buffer.length, 0);
  });
}
