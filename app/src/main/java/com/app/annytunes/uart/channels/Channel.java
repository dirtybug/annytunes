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
