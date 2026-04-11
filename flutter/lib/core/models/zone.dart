class Zone {
  final String name;
  final List<int> channelNumbers; // 1-based channel indices
  final bool changedChannels;
  final bool changedName;

  const Zone({
    this.name = '',
    this.channelNumbers = const [],
    this.changedChannels = false,
    this.changedName = false,
  });

  int get channelCount => channelNumbers.length;

  Zone copyWith({
    String? name,
    List<int>? channelNumbers,
    bool? changedChannels,
    bool? changedName,
  }) {
    return Zone(
      name: name ?? this.name,
      channelNumbers: channelNumbers ?? this.channelNumbers,
      changedChannels: changedChannels ?? this.changedChannels,
      changedName: changedName ?? this.changedName,
    );
  }

  @override
  String toString() => 'Zone($name, ${channelNumbers.length} channels)';
}
