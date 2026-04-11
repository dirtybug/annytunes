class ZoneChannels {
  final int zoneIndex; // 1-based
  final List<int> channelNumbers; // 1-based channel numbers
  final bool changed;

  const ZoneChannels({
    this.zoneIndex = 0,
    this.channelNumbers = const [],
    this.changed = false,
  });

  ZoneChannels copyWith({
    int? zoneIndex,
    List<int>? channelNumbers,
    bool? changed,
  }) {
    return ZoneChannels(
      zoneIndex: zoneIndex ?? this.zoneIndex,
      channelNumbers: channelNumbers ?? this.channelNumbers,
      changed: changed ?? this.changed,
    );
  }
}
