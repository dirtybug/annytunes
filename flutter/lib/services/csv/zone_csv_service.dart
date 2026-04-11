import 'package:annytunes/core/models/models.dart';

/// Parses and generates the Anytone zone CSV format.
///
/// Format: `ZoneName,"1 2 3 4"` where channels are space-separated integers.
class ZoneCsvService {
  static const _header = 'ZoneName,Channels';

  /// Generates CSV text from a list of zones.
  String generate(List<Zone> zones) {
    final buf = StringBuffer(_header);
    for (final z in zones) {
      buf.writeln();
      final name = z.name.replaceAll('"', ' ');
      final channels = z.channelNumbers.join(' ');
      buf.write('$name,"$channels"');
    }
    return buf.toString();
  }

  /// Parses CSV text into a list of zones.
  List<Zone> parse(String csvText) {
    final lines = csvText.split(RegExp(r'\r?\n'));
    final zones = <Zone>[];
    // Skip header
    for (var i = 1; i < lines.length; i++) {
      final line = lines[i].trim();
      if (line.isEmpty) continue;

      final comma = line.indexOf(',');
      if (comma < 0) continue;

      final name = line.substring(0, comma).trim();
      var rest = line.substring(comma + 1).trim();

      // Strip surrounding quotes
      if (rest.startsWith('"') && rest.endsWith('"')) {
        rest = rest.substring(1, rest.length - 1);
      }

      final tokens = rest.split(RegExp(r'[ ;,]+'));
      final channelNumbers = <int>[];
      for (final t in tokens) {
        final v = int.tryParse(t.trim());
        if (v != null && v >= 1) channelNumbers.add(v);
      }

      zones.add(Zone(name: name, channelNumbers: channelNumbers));
    }
    return zones;
  }
}
