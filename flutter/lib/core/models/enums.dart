enum PowerLevel {
  low,
  med,
  high,
  turbo;

  String get label => switch (this) {
        low => 'Low',
        med => 'Med',
        high => 'High',
        turbo => 'Turbo',
      };

  static PowerLevel fromIndex(int index) =>
      (index >= 0 && index < values.length) ? values[index] : low;
}

enum AdmitCriteria {
  always,
  ccFree,
  channelFree;

  String get label => switch (this) {
        always => 'Always',
        ccFree => 'CC Free',
        channelFree => 'Channel Free',
      };

  static AdmitCriteria fromIndex(int index) =>
      (index >= 0 && index < values.length) ? values[index] : always;

  static AdmitCriteria fromString(String s) => switch (s.toLowerCase()) {
        'always' => always,
        'cc free' => ccFree,
        'channel free' || 'ch free' => channelFree,
        _ => always,
      };
}

enum ChannelType {
  analog,
  digital;

  String get label => switch (this) {
        analog => 'Analog',
        digital => 'DMR',
      };
}
