class Bank {
  static const int recordSize = 64;

  final int address;
  final int sizeBytes;
  final int channels;
  final int startChannel; // 1-based
  final int endChannel; // 1-based

  const Bank({
    required this.address,
    required this.sizeBytes,
    required this.channels,
    required this.startChannel,
    required this.endChannel,
  });

  int get recordCount => sizeBytes ~/ recordSize;

  int addressOfRecord(int recIndex) {
    assert(recIndex >= 0 && recIndex < recordCount);
    return address + recIndex * recordSize;
  }
}
