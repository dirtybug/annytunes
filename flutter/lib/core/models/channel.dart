import 'enums.dart';

class Channel {
  // Core fields (parsed from radio memory)
  final String name;
  final int rxHz;
  final int txHz;
  final bool digital;
  final int colorCode;
  final int timeslot;
  final int contactId;
  final String contactName;
  final int radioIdIndex;
  final double bandwidthKHz;
  final AdmitCriteria admit;
  final PowerLevel power;
  final bool edited;

  // Extended CSV fields (not yet parsed from radio memory)
  final String ctcssDecode;
  final String ctcssEncode;
  final String contactCallType;
  final String squelchMode;
  final String optionalSignal;
  final String dtmfId;
  final String twoToneId;
  final String fiveToneId;
  final String pttId;
  final String scanList;
  final String receiveGroupList;
  final bool pttProhibit;
  final bool reverse;
  final String idleTx;
  final String slotSuit;
  final bool aesDigitalEncryption;
  final bool digitalEncryption;
  final bool callConfirmation;
  final bool talkAround;
  final bool workAlone;
  final String customCtcss;
  final String twoToneDecode;
  final bool ranging;
  final bool throughMode;
  final bool aprsRx;
  final String analogAprsPttMode;
  final String digitalAprsPttMode;
  final String aprsReportType;
  final String digitalAprsReportChannel;
  final int correctFrequencyHz;
  final bool smsConfirmation;
  final bool excludeFromRoaming;
  final String dmrMode;
  final bool dataAckDisable;
  final String r5ToneBot;
  final String r5ToneEot;
  final bool autoScan;
  final bool anaAprsMute;
  final bool sendTalkerAlias;
  final String anaAprsTxPath;
  final bool arc4;
  final String exEmgKind;
  final String idleTxAlt;
  final bool compand;
  final bool disturEn;
  final String disturFreq;
  final String rpgaMdc;
  final bool dmrCrcIgnore;
  final int txColorCode;

  const Channel({
    this.name = '',
    this.rxHz = 0,
    this.txHz = 0,
    this.digital = false,
    this.colorCode = 0,
    this.timeslot = 1,
    this.contactId = 0,
    this.contactName = '',
    this.radioIdIndex = 0,
    this.bandwidthKHz = 12.5,
    this.admit = AdmitCriteria.always,
    this.power = PowerLevel.low,
    this.edited = false,
    this.ctcssDecode = '',
    this.ctcssEncode = '',
    this.contactCallType = '',
    this.squelchMode = '',
    this.optionalSignal = '',
    this.dtmfId = '',
    this.twoToneId = '',
    this.fiveToneId = '',
    this.pttId = '',
    this.scanList = '',
    this.receiveGroupList = '',
    this.pttProhibit = false,
    this.reverse = false,
    this.idleTx = '',
    this.slotSuit = '',
    this.aesDigitalEncryption = false,
    this.digitalEncryption = false,
    this.callConfirmation = false,
    this.talkAround = false,
    this.workAlone = false,
    this.customCtcss = '',
    this.twoToneDecode = '',
    this.ranging = false,
    this.throughMode = false,
    this.aprsRx = false,
    this.analogAprsPttMode = '',
    this.digitalAprsPttMode = '',
    this.aprsReportType = '',
    this.digitalAprsReportChannel = '',
    this.correctFrequencyHz = 0,
    this.smsConfirmation = false,
    this.excludeFromRoaming = false,
    this.dmrMode = '',
    this.dataAckDisable = false,
    this.r5ToneBot = '',
    this.r5ToneEot = '',
    this.autoScan = false,
    this.anaAprsMute = false,
    this.sendTalkerAlias = false,
    this.anaAprsTxPath = '',
    this.arc4 = false,
    this.exEmgKind = '',
    this.idleTxAlt = '',
    this.compand = false,
    this.disturEn = false,
    this.disturFreq = '',
    this.rpgaMdc = '',
    this.dmrCrcIgnore = false,
    this.txColorCode = 0,
  });

  bool get isEmpty => name.isEmpty && rxHz == 0;

  ChannelType get type => digital ? ChannelType.digital : ChannelType.analog;

  String get rxMhzFormatted => _formatHz(rxHz);
  String get txMhzFormatted => _formatHz(txHz);

  static String _formatHz(int hz) {
    if (hz <= 0) return '-';
    return '${(hz / 1000000).toStringAsFixed(5)} MHz';
  }

  Channel copyWith({
    String? name,
    int? rxHz,
    int? txHz,
    bool? digital,
    int? colorCode,
    int? timeslot,
    int? contactId,
    String? contactName,
    int? radioIdIndex,
    double? bandwidthKHz,
    AdmitCriteria? admit,
    PowerLevel? power,
    bool? edited,
    String? ctcssDecode,
    String? ctcssEncode,
    String? contactCallType,
    String? squelchMode,
    String? optionalSignal,
    String? dtmfId,
    String? twoToneId,
    String? fiveToneId,
    String? pttId,
    String? scanList,
    String? receiveGroupList,
    bool? pttProhibit,
    bool? reverse,
    String? idleTx,
    String? slotSuit,
    bool? aesDigitalEncryption,
    bool? digitalEncryption,
    bool? callConfirmation,
    bool? talkAround,
    bool? workAlone,
    String? customCtcss,
    String? twoToneDecode,
    bool? ranging,
    bool? throughMode,
    bool? aprsRx,
    String? analogAprsPttMode,
    String? digitalAprsPttMode,
    String? aprsReportType,
    String? digitalAprsReportChannel,
    int? correctFrequencyHz,
    bool? smsConfirmation,
    bool? excludeFromRoaming,
    String? dmrMode,
    bool? dataAckDisable,
    String? r5ToneBot,
    String? r5ToneEot,
    bool? autoScan,
    bool? anaAprsMute,
    bool? sendTalkerAlias,
    String? anaAprsTxPath,
    bool? arc4,
    String? exEmgKind,
    String? idleTxAlt,
    bool? compand,
    bool? disturEn,
    String? disturFreq,
    String? rpgaMdc,
    bool? dmrCrcIgnore,
    int? txColorCode,
  }) {
    return Channel(
      name: name ?? this.name,
      rxHz: rxHz ?? this.rxHz,
      txHz: txHz ?? this.txHz,
      digital: digital ?? this.digital,
      colorCode: colorCode ?? this.colorCode,
      timeslot: timeslot ?? this.timeslot,
      contactId: contactId ?? this.contactId,
      contactName: contactName ?? this.contactName,
      radioIdIndex: radioIdIndex ?? this.radioIdIndex,
      bandwidthKHz: bandwidthKHz ?? this.bandwidthKHz,
      admit: admit ?? this.admit,
      power: power ?? this.power,
      edited: edited ?? this.edited,
      ctcssDecode: ctcssDecode ?? this.ctcssDecode,
      ctcssEncode: ctcssEncode ?? this.ctcssEncode,
      contactCallType: contactCallType ?? this.contactCallType,
      squelchMode: squelchMode ?? this.squelchMode,
      optionalSignal: optionalSignal ?? this.optionalSignal,
      dtmfId: dtmfId ?? this.dtmfId,
      twoToneId: twoToneId ?? this.twoToneId,
      fiveToneId: fiveToneId ?? this.fiveToneId,
      pttId: pttId ?? this.pttId,
      scanList: scanList ?? this.scanList,
      receiveGroupList: receiveGroupList ?? this.receiveGroupList,
      pttProhibit: pttProhibit ?? this.pttProhibit,
      reverse: reverse ?? this.reverse,
      idleTx: idleTx ?? this.idleTx,
      slotSuit: slotSuit ?? this.slotSuit,
      aesDigitalEncryption: aesDigitalEncryption ?? this.aesDigitalEncryption,
      digitalEncryption: digitalEncryption ?? this.digitalEncryption,
      callConfirmation: callConfirmation ?? this.callConfirmation,
      talkAround: talkAround ?? this.talkAround,
      workAlone: workAlone ?? this.workAlone,
      customCtcss: customCtcss ?? this.customCtcss,
      twoToneDecode: twoToneDecode ?? this.twoToneDecode,
      ranging: ranging ?? this.ranging,
      throughMode: throughMode ?? this.throughMode,
      aprsRx: aprsRx ?? this.aprsRx,
      analogAprsPttMode: analogAprsPttMode ?? this.analogAprsPttMode,
      digitalAprsPttMode: digitalAprsPttMode ?? this.digitalAprsPttMode,
      aprsReportType: aprsReportType ?? this.aprsReportType,
      digitalAprsReportChannel:
          digitalAprsReportChannel ?? this.digitalAprsReportChannel,
      correctFrequencyHz: correctFrequencyHz ?? this.correctFrequencyHz,
      smsConfirmation: smsConfirmation ?? this.smsConfirmation,
      excludeFromRoaming: excludeFromRoaming ?? this.excludeFromRoaming,
      dmrMode: dmrMode ?? this.dmrMode,
      dataAckDisable: dataAckDisable ?? this.dataAckDisable,
      r5ToneBot: r5ToneBot ?? this.r5ToneBot,
      r5ToneEot: r5ToneEot ?? this.r5ToneEot,
      autoScan: autoScan ?? this.autoScan,
      anaAprsMute: anaAprsMute ?? this.anaAprsMute,
      sendTalkerAlias: sendTalkerAlias ?? this.sendTalkerAlias,
      anaAprsTxPath: anaAprsTxPath ?? this.anaAprsTxPath,
      arc4: arc4 ?? this.arc4,
      exEmgKind: exEmgKind ?? this.exEmgKind,
      idleTxAlt: idleTxAlt ?? this.idleTxAlt,
      compand: compand ?? this.compand,
      disturEn: disturEn ?? this.disturEn,
      disturFreq: disturFreq ?? this.disturFreq,
      rpgaMdc: rpgaMdc ?? this.rpgaMdc,
      dmrCrcIgnore: dmrCrcIgnore ?? this.dmrCrcIgnore,
      txColorCode: txColorCode ?? this.txColorCode,
    );
  }

  @override
  String toString() {
    final mode = digital ? 'DMR' : 'Analog';
    final cc = digital ? '$colorCode' : '-';
    final ts = digital ? '$timeslot' : '-';
    return 'Channel($name, RX: $rxMhzFormatted, TX: $txMhzFormatted, '
        'Mode: $mode, CC: $cc, TS: $ts, Power: ${power.label})';
  }
}
