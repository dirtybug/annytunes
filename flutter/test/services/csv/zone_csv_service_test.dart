import 'package:flutter_test/flutter_test.dart';
import 'package:annytunes/core/models/models.dart';
import 'package:annytunes/services/csv/zone_csv_service.dart';

void main() {
  late ZoneCsvService service;

  setUp(() {
    service = ZoneCsvService();
  });

  group('ZoneCsvService', () {
    test('generate produces header as first line', () {
      final csv = service.generate([]);
      expect(csv, startsWith('ZoneName,Channels'));
    });

    test('round-trip: generate then parse preserves zone data', () {
      final zones = [
        const Zone(name: 'Local Repeaters', channelNumbers: [1, 2, 3, 4, 10]),
        const Zone(name: 'Simplex', channelNumbers: [5, 6]),
      ];

      final csv = service.generate(zones);
      final parsed = service.parse(csv);

      expect(parsed, hasLength(2));
      expect(parsed[0].name, 'Local Repeaters');
      expect(parsed[0].channelNumbers, [1, 2, 3, 4, 10]);
      expect(parsed[1].name, 'Simplex');
      expect(parsed[1].channelNumbers, [5, 6]);
    });

    test('parse handles empty input', () {
      expect(service.parse(''), isEmpty);
    });

    test('parse handles header-only input', () {
      expect(service.parse('ZoneName,Channels'), isEmpty);
    });

    test('parse handles zone with no channels', () {
      final csv = 'ZoneName,Channels\nEmpty Zone,""';
      final parsed = service.parse(csv);
      expect(parsed, hasLength(1));
      expect(parsed[0].name, 'Empty Zone');
      expect(parsed[0].channelNumbers, isEmpty);
    });

    test('generate with empty channel list produces empty quotes', () {
      final csv = service.generate([
        const Zone(name: 'Empty', channelNumbers: []),
      ]);
      expect(csv, contains('Empty,""'));
    });

    test('parse handles semicolon-separated channels', () {
      final csv = 'ZoneName,Channels\nTest,"1;2;3"';
      final parsed = service.parse(csv);
      expect(parsed[0].channelNumbers, [1, 2, 3]);
    });

    test('parse skips invalid channel numbers', () {
      final csv = 'ZoneName,Channels\nTest,"1 abc 3 0 5"';
      final parsed = service.parse(csv);
      // 0 is skipped (must be >= 1), abc is skipped
      expect(parsed[0].channelNumbers, [1, 3, 5]);
    });

    test('parse skips lines without comma', () {
      final csv = 'ZoneName,Channels\nno comma here\nGood,"1 2"';
      final parsed = service.parse(csv);
      expect(parsed, hasLength(1));
      expect(parsed[0].name, 'Good');
    });

    test('generate strips quotes from zone name', () {
      final csv = service.generate([
        const Zone(name: 'Zone "A"', channelNumbers: [1]),
      ]);
      // Quotes replaced with spaces
      expect(csv, contains('Zone  A '));
    });

    test('parse handles Windows-style line endings', () {
      final csv = 'ZoneName,Channels\r\nLocal,"1 2 3"\r\n';
      final parsed = service.parse(csv);
      expect(parsed, hasLength(1));
      expect(parsed[0].name, 'Local');
      expect(parsed[0].channelNumbers, [1, 2, 3]);
    });
  });
}
