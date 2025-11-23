package com.app.annytunes.uart.channels;

public class Channel {
    public String name;       // ASCII (model-dependent length, commonly 16)
    public long rxHz;         // RX frequency in Hz
    public long txHz;         // TX frequency in Hz
    public boolean digital;   // true=DMR, false=Analog
    public int colorCode;     // DMR CC 0..15
    public int timeslot;      // 1 or 2 for DMR
    public int contactId;     // DMR TG/priv id
    public String contactName;
	public int radioIdIndex;
	public double bandwidthKHz;
	public String admit;
	public int power;
	public boolean edited; // user edited marker
    // CSV extended fields (added per full header); many are not yet parsed from radio memory
    public String ctcssDecode;        // CTCSS/DCS Decode (MISSING: not parsed)
    public String ctcssEncode;        // CTCSS/DCS Encode (MISSING: not parsed)
    public String contactCallType;    // Contact Call Type (MISSING)
    public String squelchMode;        // Squelch Mode (MISSING)
    public String optionalSignal;     // Optional Signal (MISSING)
    public String dtmfId;             // DTMF ID (MISSING)
    public String twoToneId;          // 2Tone ID (MISSING)
    public String fiveToneId;         // 5Tone ID (MISSING)
    public String pttId;              // PTT ID (MISSING)
    public String scanList;           // Scan List (MISSING)
    public String receiveGroupList;   // Receive Group List (MISSING)
    public boolean pttProhibit;       // PTT Prohibit (MISSING)
    public boolean reverse;           // Reverse (MISSING)
    public String idleTx;             // Idle TX (MISSING)
    public String slotSuit;           // Slot Suit (MISSING)
    public boolean aesDigitalEncryption; // AES Digital Encryption (MISSING)
    public boolean digitalEncryption;    // Digital Encryption (MISSING)
    public boolean callConfirmation;     // Call Confirmation (MISSING)
    public boolean talkAround;           // Talk Around(Simplex) (MISSING)
    public boolean workAlone;            // Work Alone (MISSING)
    public String customCtcss;           // Custom CTCSS (MISSING)
    public String twoToneDecode;         // 2TONE Decode (MISSING)
    public boolean ranging;              // Ranging (MISSING)
    public boolean throughMode;          // Through Mode (MISSING)
    public boolean aprsRx;               // APRS RX (MISSING)
    public String analogAprsPttMode;     // Analog APRS PTT Mode (MISSING)
    public String digitalAprsPttMode;    // Digital APRS PTT Mode (MISSING)
    public String aprsReportType;        // APRS Report Type (MISSING)
    public String digitalAprsReportChannel; // Digital APRS Report Channel (MISSING)
    public long correctFrequencyHz;      // Correct Frequency[Hz] (MISSING)
    public boolean smsConfirmation;      // SMS Confirmation (MISSING)
    public boolean excludeFromRoaming;   // Exclude channel from roaming (MISSING)
    public String dmrMode;               // DMR MODE (MISSING)
    public boolean dataAckDisable;       // DataACK Disable (MISSING)
    public String r5ToneBot;             // R5ToneBot (MISSING)
    public String r5ToneEot;             // R5ToneEot (MISSING)
    public boolean autoScan;             // Auto Scan (MISSING)
    public boolean anaAprsMute;          // Ana Aprs Mute (MISSING)
    public boolean sendTalkerAlias;      // Send Talker Aias (MISSING)
    public String anaAprsTxPath;         // AnaAprsTxPath (MISSING)
    public boolean arc4;                 // ARC4 (MISSING)
    public String exEmgKind;             // ex_emg_kind (MISSING)
    public String idle_tx;               // idle_tx (alternative naming) (MISSING)
    public boolean compand;              // Compand (MISSING)
    public boolean disturEn;             // DisturEn (MISSING)
    public String disturFreq;            // DisturFreq (MISSING)
    public String rpgaMdc;               // Rpga_Mdc (MISSING)
    public boolean dmrCrcIgnore;         // dmr_crc_ignore (MISSING)
    public int txColorCode;              // TxCc (MISSING separate TX color code)

    @Override
    public String toString() {
	    String mode = digital ? "DMR" : "Analog";
	    String cc   = digital ? Integer.toString(colorCode) : "-";
	    String ts   = digital ? Integer.toString(timeslot)  : "-";
	    String pow  = powerLabel(power);
	    String adm  = (admit == null || admit.isBlank()) ? "-" : admit;
	    String bw   = (bandwidthKHz > 0) ? String.format(java.util.Locale.ROOT, "%.1f", bandwidthKHz) : "-";

	    return String.format(java.util.Locale.ROOT,
	        "Name: %s, Rx: %s, Tx: %s, Mode: %s, CC: %s, TS: %s, Contact ID: %d, Radio ID Index: %d, Bandwidth: %s kHz, Admit: %s, Power: %s",
	        (name == null ? "" : name),
	        fmtHz(rxHz),
	        fmtHz(txHz),
	        mode, cc, ts,
	        contactId, radioIdIndex,
	        bw, adm, pow
	    );
	}

	private static String fmtHz(long hz) {
	    if (hz <= 0) return "-";
	    double mhz = hz / 1_000_000.0;
	    return String.format(java.util.Locale.ROOT, "%.5f MHz", mhz);
	}

	private static String powerLabel(int p) {
	    switch (p) {
	        case 0:  return "Low";
	        case 1:  return "Med";
	        case 2:  return "High";
	        case 3:  return "Turbo";
	        default: return "P" + p;
	    }
	}

}
