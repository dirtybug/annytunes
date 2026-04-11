import 'package:flutter_test/flutter_test.dart';
import 'package:annytunes/core/models/models.dart';
import 'package:annytunes/services/csv/channel_csv_service.dart';

void main() {
  late ChannelCsvService service;

  setUp(() {
    service = ChannelCsvService();
  });

  group('ChannelCsvService', () {
    test('generate produces header as first line', () {
      final csv = service.generate([]);
      expect(csv, startsWith('No.,Channel Name,Receive Frequency'));
    });

    test('round-trip: generate then parse preserves channel data', () {
      final channel = Channel(
        name: 'Test Repeater',
        rxHz: 145500000,
        txHz: 144900000,
        digital: false,
        power: PowerLevel.high,
        bandwidthKHz: 25.0,
        colorCode: 5,
        timeslot: 2,
        radioIdIndex: 1,
        contactName: 'TG1',
        pttProhibit: true,
        reverse: false,
        compand: true,
        txColorCode: 3,
        admit: AdmitCriteria.channelFree,
      );

      final csv = service.generate([channel]);
      final parsed = service.parse(csv);

      expect(parsed, hasLength(1));
      final c = parsed[0];
      expect(c.name, 'Test Repeater');
      expect(c.rxHz, 145500000);
      expect(c.txHz, 144900000);
      expect(c.digital, false);
      expect(c.power, PowerLevel.high);
      expect(c.bandwidthKHz, 25.0);
      expect(c.colorCode, 5);
      expect(c.timeslot, 2);
      expect(c.radioIdIndex, 1);
      expect(c.contactName, 'TG1');
      expect(c.pttProhibit, true);
      expect(c.reverse, false);
      expect(c.compand, true);
      expect(c.txColorCode, 3);
      expect(c.admit, AdmitCriteria.channelFree);
    });

    test('round-trip with digital channel', () {
      final channel = Channel(
        name: 'DMR Repeater',
        rxHz: 439500000,
        txHz: 431900000,
        digital: true,
        power: PowerLevel.turbo,
        bandwidthKHz: 12.5,
        colorCode: 10,
        timeslot: 1,
      );

      final csv = service.generate([channel]);
      final parsed = service.parse(csv);

      expect(parsed, hasLength(1));
      expect(parsed[0].digital, true);
      expect(parsed[0].power, PowerLevel.turbo);
      expect(parsed[0].bandwidthKHz, 12.5);
      expect(parsed[0].colorCode, 10);
      expect(parsed[0].timeslot, 1);
    });

    test('round-trip preserves multiple channels with correct row numbers', () {
      final channels = [
        const Channel(name: 'Ch1', rxHz: 145500000, txHz: 144900000),
        const Channel(name: 'Ch2', rxHz: 146500000, txHz: 145900000),
        const Channel(name: 'Ch3', rxHz: 147500000, txHz: 146900000),
      ];

      final csv = service.generate(channels);
      final parsed = service.parse(csv);

      expect(parsed, hasLength(3));
      expect(parsed[0].name, 'Ch1');
      expect(parsed[1].name, 'Ch2');
      expect(parsed[2].name, 'Ch3');
    });

    test('parse handles empty input', () {
      expect(service.parse(''), isEmpty);
    });

    test('parse handles header-only input', () {
      final csv = service.generate([]);
      expect(service.parse(csv), isEmpty);
    });

    test('parse handles short rows by padding', () {
      // Only provide a few columns -- rest should default
      final csv = 'No.,Channel Name,Receive Frequency\n1,ShortRow,145.50000';
      final parsed = service.parse(csv);
      expect(parsed, hasLength(1));
      expect(parsed[0].name, 'ShortRow');
      expect(parsed[0].rxHz, 145500000);
    });

    test('frequency formatting uses 5 decimal places', () {
      final csv = service.generate([
        const Channel(name: 'Freq Test', rxHz: 145500000, txHz: 0),
      ]);
      expect(csv, contains('145.50000'));
    });

    test('frequency formatting omits zero frequencies', () {
      final csv = service.generate([
        const Channel(name: 'No TX', rxHz: 145500000, txHz: 0),
      ]);
      final parsed = service.parse(csv);
      expect(parsed[0].txHz, 0);
    });

    test('boolean fields round-trip as Yes/No', () {
      final csv = service.generate([
        const Channel(
          name: 'Bools',
          pttProhibit: true,
          reverse: false,
          workAlone: true,
          dmrCrcIgnore: true,
        ),
      ]);

      expect(csv, contains('Yes'));
      expect(csv, contains('No'));

      final parsed = service.parse(csv);
      expect(parsed[0].pttProhibit, true);
      expect(parsed[0].reverse, false);
      expect(parsed[0].workAlone, true);
      expect(parsed[0].dmrCrcIgnore, true);
    });

    test('power level labels match Java format', () {
      for (final p in PowerLevel.values) {
        final csv = service.generate([Channel(name: 'P', power: p)]);
        final parsed = service.parse(csv);
        expect(parsed[0].power, p);
      }
    });

    test('bandwidth labels match Java format', () {
      final csv125 = service.generate([
        const Channel(name: 'BW', bandwidthKHz: 12.5),
      ]);
      expect(csv125, contains('12.5K'));

      final csv25 = service.generate([
        const Channel(name: 'BW', bandwidthKHz: 25.0),
      ]);
      expect(csv25, contains('25K'));
    });

    test('channel name with commas and quotes survives round-trip', () {
      final csv = service.generate([
        const Channel(name: 'Name, with "quotes"'),
      ]);
      final parsed = service.parse(csv);
      expect(parsed[0].name, 'Name, with "quotes"');
    });
  });
}
