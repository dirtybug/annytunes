import 'package:csv/csv.dart';

import 'package:annytunes/core/models/models.dart';

/// Parses and generates the 61-column Anytone channel CSV format.
class ChannelCsvService {
  static const _header = [
    'No.', 'Channel Name', 'Receive Frequency', 'Transmit Frequency',
    'Channel Type', 'Transmit Power', 'Band Width',
    'CTCSS/DCS Decode', 'CTCSS/DCS Encode', 'Contact', 'Contact Call Type',
    'Radio ID', 'Busy Lock/TX Permit', 'Squelch Mode', 'Optional Signal',
    'DTMF ID', '2Tone ID', '5Tone ID', 'PTT ID', 'RX Color Code', 'Slot',
    'Scan List', 'Receive Group List', 'PTT Prohibit', 'Reverse', 'Idle TX',
    'Slot Suit', 'AES Digital Encryption', 'Digital Encryption',
    'Call Confirmation', 'Talk Around(Simplex)', 'Work Alone', 'Custom CTCSS',
    '2TONE Decode', 'Ranging', 'Through Mode', 'APRS RX',
    'Analog APRS PTT Mode', 'Digital APRS PTT Mode', 'APRS Report Type',
    'Digital APRS Report Channel', 'Correct Frequency[Hz]',
    'SMS Confirmation', 'Exclude channel from roaming', 'DMR MODE',
    'DataACK Disable', 'R5ToneBot', 'R5ToneEot', 'Auto Scan',
    'Ana Aprs Mute', 'Send Talker Aias', 'AnaAprsTxPath', 'ARC4',
    'ex_emg_kind', 'idle_tx', 'Compand', 'DisturEn', 'DisturFreq',
    'Rpga_Mdc', 'dmr_crc_ignore', 'TxCc',
  ];

  static const _columnCount = 61;

  /// Generates CSV text from a list of channels.
  String generate(List<Channel> channels) {
    final rows = <List<String>>[_header];
    for (var i = 0; i < channels.length; i++) {
      rows.add(_channelToRow(channels[i], i + 1));
    }
    return const ListToCsvConverter().convert(rows);
  }

  /// Parses CSV text into a list of channels.
  List<Channel> parse(String csvText) {
    final rows = const CsvToListConverter(eol: '\n', shouldParseNumbers: false)
        .convert(csvText);
    if (rows.isEmpty) return [];

    final channels = <Channel>[];
    // Skip header row
    for (var i = 1; i < rows.length; i++) {
      final row = rows[i];
      if (row.isEmpty || (row.length == 1 && row[0].toString().trim().isEmpty)) {
        continue;
      }
      // Pad to expected column count
      final parts = List<String>.generate(
        _columnCount,
        (j) => j < row.length ? row[j].toString() : '',
      );
      channels.add(_rowToChannel(parts));
    }
    return channels;
  }

  List<String> _channelToRow(Channel c, int index) {
    return [
      index.toString(),
      c.name,
      _fmtFreq(c.rxHz),
      _fmtFreq(c.txHz),
      c.digital ? 'Digital' : 'Analog',
      _powerLabel(c.power),
      _fmtBandwidth(c.bandwidthKHz),
      c.ctcssDecode,
      c.ctcssEncode,
      c.contactName,
      c.contactCallType,
      c.radioIdIndex.toString(),
      c.admit.label,
      c.squelchMode,
      c.optionalSignal,
      c.dtmfId,
      c.twoToneId,
      c.fiveToneId,
      c.pttId,
      c.colorCode.toString(),
      c.timeslot.toString(),
      c.scanList,
      c.receiveGroupList,
      _boolLabel(c.pttProhibit),
      _boolLabel(c.reverse),
      c.idleTx,
      c.slotSuit,
      _boolLabel(c.aesDigitalEncryption),
      _boolLabel(c.digitalEncryption),
      _boolLabel(c.callConfirmation),
      _boolLabel(c.talkAround),
      _boolLabel(c.workAlone),
      c.customCtcss,
      c.twoToneDecode,
      _boolLabel(c.ranging),
      _boolLabel(c.throughMode),
      _boolLabel(c.aprsRx),
      c.analogAprsPttMode,
      c.digitalAprsPttMode,
      c.aprsReportType,
      c.digitalAprsReportChannel,
      c.correctFrequencyHz.toString(),
      _boolLabel(c.smsConfirmation),
      _boolLabel(c.excludeFromRoaming),
      c.dmrMode,
      _boolLabel(c.dataAckDisable),
      c.r5ToneBot,
      c.r5ToneEot,
      _boolLabel(c.autoScan),
      _boolLabel(c.anaAprsMute),
      _boolLabel(c.sendTalkerAlias),
      c.anaAprsTxPath,
      _boolLabel(c.arc4),
      c.exEmgKind,
      c.idleTxAlt,
      _boolLabel(c.compand),
      _boolLabel(c.disturEn),
      c.disturFreq,
      c.rpgaMdc,
      _boolLabel(c.dmrCrcIgnore),
      c.txColorCode.toString(),
    ];
  }

  Channel _rowToChannel(List<String> p) {
    return Channel(
      name: p[1],
      rxHz: _parseFreq(p[2]),
      txHz: _parseFreq(p[3]),
      digital: _parseDigital(p[4]),
      power: _parsePower(p[5]),
      bandwidthKHz: _parseBandwidth(p[6]),
      ctcssDecode: p[7],
      ctcssEncode: p[8],
      contactName: p[9],
      contactCallType: p[10],
      radioIdIndex: _parseInt(p[11]),
      admit: AdmitCriteria.fromString(p[12]),
      squelchMode: p[13],
      optionalSignal: p[14],
      dtmfId: p[15],
      twoToneId: p[16],
      fiveToneId: p[17],
      pttId: p[18],
      colorCode: _parseInt(p[19]),
      timeslot: _parseInt(p[20]),
      scanList: p[21],
      receiveGroupList: p[22],
      pttProhibit: _parseBool(p[23]),
      reverse: _parseBool(p[24]),
      idleTx: p[25],
      slotSuit: p[26],
      aesDigitalEncryption: _parseBool(p[27]),
      digitalEncryption: _parseBool(p[28]),
      callConfirmation: _parseBool(p[29]),
      talkAround: _parseBool(p[30]),
      workAlone: _parseBool(p[31]),
      customCtcss: p[32],
      twoToneDecode: p[33],
      ranging: _parseBool(p[34]),
      throughMode: _parseBool(p[35]),
      aprsRx: _parseBool(p[36]),
      analogAprsPttMode: p[37],
      digitalAprsPttMode: p[38],
      aprsReportType: p[39],
      digitalAprsReportChannel: p[40],
      correctFrequencyHz: _parseInt(p[41]),
      smsConfirmation: _parseBool(p[42]),
      excludeFromRoaming: _parseBool(p[43]),
      dmrMode: p[44],
      dataAckDisable: _parseBool(p[45]),
      r5ToneBot: p[46],
      r5ToneEot: p[47],
      autoScan: _parseBool(p[48]),
      anaAprsMute: _parseBool(p[49]),
      sendTalkerAlias: _parseBool(p[50]),
      anaAprsTxPath: p[51],
      arc4: _parseBool(p[52]),
      exEmgKind: p[53],
      idleTxAlt: p[54],
      compand: _parseBool(p[55]),
      disturEn: _parseBool(p[56]),
      disturFreq: p[57],
      rpgaMdc: p[58],
      dmrCrcIgnore: _parseBool(p[59]),
      txColorCode: _parseInt(p[60]),
    );
  }

  // -- Formatting helpers --

  static String _fmtFreq(int hz) {
    if (hz <= 0) return '';
    return (hz / 1000000).toStringAsFixed(5);
  }

  static String _powerLabel(PowerLevel p) => switch (p) {
        PowerLevel.low => 'Low',
        PowerLevel.med => 'Mid',
        PowerLevel.high => 'High',
        PowerLevel.turbo => 'Turbo',
      };

  static String _fmtBandwidth(double bw) {
    if (bw <= 0) return '';
    if ((bw - 12.5).abs() < 0.01) return '12.5K';
    if ((bw - 25.0).abs() < 0.01) return '25K';
    return '${bw.toStringAsFixed(1)}K';
  }

  static String _boolLabel(bool b) => b ? 'Yes' : 'No';

  // -- Parsing helpers --

  static int _parseFreq(String s) {
    s = s.trim().replaceAll('MHz', '').trim();
    if (s.isEmpty) return 0;
    final mhz = double.tryParse(s);
    if (mhz != null) return (mhz * 1000000).round();
    return int.tryParse(s) ?? 0;
  }

  static bool _parseDigital(String s) =>
      s.trim().toLowerCase().startsWith('digi');

  static PowerLevel _parsePower(String s) => switch (s.trim().toLowerCase()) {
        'low' => PowerLevel.low,
        'mid' || 'medium' => PowerLevel.med,
        'high' => PowerLevel.high,
        'turbo' => PowerLevel.turbo,
        _ => PowerLevel.low,
      };

  static double _parseBandwidth(String s) {
    s = s.trim().toUpperCase();
    if (s.endsWith('K')) s = s.substring(0, s.length - 1);
    return double.tryParse(s) ?? 0.0;
  }

  static bool _parseBool(String s) {
    final v = s.trim().toLowerCase();
    return v == '1' || v == 'true' || v == 'yes' || v == 'y';
  }

  static int _parseInt(String s) => int.tryParse(s.trim()) ?? 0;
}
