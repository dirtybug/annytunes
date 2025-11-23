package com.app.annytunes.uart.channels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvChannelUtil {
    private static final String FULL_HEADER = "\"No.\",\"Channel Name\",\"Receive Frequency\",\"Transmit Frequency\",\"Channel Type\",\"Transmit Power\",\"Band Width\",\"CTCSS/DCS Decode\",\"CTCSS/DCS Encode\",\"Contact\",\"Contact Call Type\",\"Radio ID\",\"Busy Lock/TX Permit\",\"Squelch Mode\",\"Optional Signal\",\"DTMF ID\",\"2Tone ID\",\"5Tone ID\",\"PTT ID\",\"RX Color Code\",\"Slot\",\"Scan List\",\"Receive Group List\",\"PTT Prohibit\",\"Reverse\",\"Idle TX\",\"Slot Suit\",\"AES Digital Encryption\",\"Digital Encryption\",\"Call Confirmation\",\"Talk Around(Simplex)\",\"Work Alone\",\"Custom CTCSS\",\"2TONE Decode\",\"Ranging\",\"Through Mode\",\"APRS RX\",\"Analog APRS PTT Mode\",\"Digital APRS PTT Mode\",\"APRS Report Type\",\"Digital APRS Report Channel\",\"Correct Frequency[Hz]\",\"SMS Confirmation\",\"Exclude channel from roaming\",\"DMR MODE\",\"DataACK Disable\",\"R5ToneBot\",\"R5ToneEot\",\"Auto Scan\",\"Ana Aprs Mute\",\"Send Talker Aias\",\"AnaAprsTxPath\",\"ARC4\",\"ex_emg_kind\",\"idle_tx\",\"Compand\",\"DisturEn\",\"DisturFreq\",\"Rpga_Mdc\",\"dmr_crc_ignore\",\"TxCc\"";

    public static void write(List<Channel> list, File f) throws IOException {
        if (list == null) list = List.of();
	    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            pw.println(FULL_HEADER);
            int idx = 1;
            for (Channel c : list) pw.println(row(c, idx++));
	    }
	}

    public static List<Channel> read(File f) throws IOException {
        List<Channel> out = new ArrayList<>();
        if (f == null || !f.exists()) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 61) { // pad missing columns
                    String[] padded = new String[61];
                    System.arraycopy(parts, 0, padded, 0, parts.length);
                    for (int i = parts.length; i < 61; i++) padded[i] = "";
                    parts = padded;
                }
                Channel c = new Channel();
                c.name = unesc(parts[1]);
                c.rxHz = parseFreq(parts[2]);
                c.txHz = parseFreq(parts[3]);
                c.digital = parseDigital(parts, 4);
                c.power = parsePower(parts, 5);
                c.bandwidthKHz = parseBandwidth(parts[6]);
                c.ctcssDecode = unesc(parts[7]);
                c.ctcssEncode = unesc(parts[8]);
                c.contactName = unesc(parts[9]);
                c.contactCallType = unesc(parts[10]);
                c.radioIdIndex = parseIntSafe(parts, 11);
                c.admit = unesc(parts[12]);
                c.squelchMode = unesc(parts[13]);
                c.optionalSignal = unesc(parts[14]);
                c.dtmfId = unesc(parts[15]);
                c.twoToneId = unesc(parts[16]);
                c.fiveToneId = unesc(parts[17]);
                c.pttId = unesc(parts[18]);
                c.colorCode = parseIntSafe(parts, 19);
                c.timeslot = parseIntSafe(parts, 20);
                c.scanList = unesc(parts[21]);
                c.receiveGroupList = unesc(parts[22]);
                c.pttProhibit = parseBool(parts[23]);
                c.reverse = parseBool(parts[24]);
                c.idleTx = unesc(parts[25]);
                c.slotSuit = unesc(parts[26]);
                c.aesDigitalEncryption = parseBool(parts[27]);
                c.digitalEncryption = parseBool(parts[28]);
                c.callConfirmation = parseBool(parts[29]);
                c.talkAround = parseBool(parts[30]);
                c.workAlone = parseBool(parts[31]);
                c.customCtcss = unesc(parts[32]);
                c.twoToneDecode = unesc(parts[33]);
                c.ranging = parseBool(parts[34]);
                c.throughMode = parseBool(parts[35]);
                c.aprsRx = parseBool(parts[36]);
                c.analogAprsPttMode = unesc(parts[37]);
                c.digitalAprsPttMode = unesc(parts[38]);
                c.aprsReportType = unesc(parts[39]);
                c.digitalAprsReportChannel = unesc(parts[40]);
                c.correctFrequencyHz = parseLongSafe(parts, 41);
                c.smsConfirmation = parseBool(parts[42]);
                c.excludeFromRoaming = parseBool(parts[43]);
                c.dmrMode = unesc(parts[44]);
                c.dataAckDisable = parseBool(parts[45]);
                c.r5ToneBot = unesc(parts[46]);
                c.r5ToneEot = unesc(parts[47]);
                c.autoScan = parseBool(parts[48]);
                c.anaAprsMute = parseBool(parts[49]);
                c.sendTalkerAlias = parseBool(parts[50]);
                c.anaAprsTxPath = unesc(parts[51]);
                c.arc4 = parseBool(parts[52]);
                c.exEmgKind = unesc(parts[53]);
                c.idle_tx = unesc(parts[54]);
                c.compand = parseBool(parts[55]);
                c.disturEn = parseBool(parts[56]);
                c.disturFreq = unesc(parts[57]);
                c.rpgaMdc = unesc(parts[58]);
                c.dmrCrcIgnore = parseBool(parts[59]);
                c.txColorCode = parseIntSafe(parts, 60);
                out.add(c);
            }
        }
        return out;
    }

    public static String header() {
        return FULL_HEADER;
    }

    public static String row(Channel c) {
        return row(c, -1);
    }

    public static String row(Channel c, int index) {
        if (c == null) c = new Channel();
        StringBuilder sb = new StringBuilder();
        java.util.function.Consumer<String> q = (v) -> {
            sb.append('"').append(v == null ? "" : v.replace("\"", "\"\"")).append('"').append(',');
        };
        java.util.function.Consumer<Boolean> qb = (b) -> q.accept(boolLabel(b));
        java.util.function.Consumer<Long> ql = (l) -> q.accept(fmtFreq(l));
        java.util.function.Consumer<Integer> qi = (i) -> q.accept(Integer.toString(i));
        java.util.function.Consumer<Double> qd = (d) -> q.accept(fmtBandwidth(d));
        q.accept(index > 0 ? Integer.toString(index) : "");
        q.accept(safe(c.name));
        ql.accept(c.rxHz);
        ql.accept(c.txHz);
        q.accept(c.digital ? "Digital" : "Analog");
        q.accept(powerLabel(c.power));
        qd.accept(c.bandwidthKHz);
        q.accept(safe(c.ctcssDecode));
        q.accept(safe(c.ctcssEncode));
        q.accept(safe(c.contactName));
        q.accept(safe(c.contactCallType));
        qi.accept(c.radioIdIndex);
        q.accept(safe(c.admit));
        q.accept(safe(c.squelchMode));
        q.accept(safe(c.optionalSignal));
        q.accept(safe(c.dtmfId));
        q.accept(safe(c.twoToneId));
        q.accept(safe(c.fiveToneId));
        q.accept(safe(c.pttId));
        qi.accept(c.colorCode);
        qi.accept(c.timeslot);
        q.accept(safe(c.scanList));
        q.accept(safe(c.receiveGroupList));
        qb.accept(c.pttProhibit);
        qb.accept(c.reverse);
        q.accept(safe(c.idleTx));
        q.accept(safe(c.slotSuit));
        qb.accept(c.aesDigitalEncryption);
        qb.accept(c.digitalEncryption);
        qb.accept(c.callConfirmation);
        qb.accept(c.talkAround);
        qb.accept(c.workAlone);
        q.accept(safe(c.customCtcss));
        q.accept(safe(c.twoToneDecode));
        qb.accept(c.ranging);
        qb.accept(c.throughMode);
        qb.accept(c.aprsRx);
        q.accept(safe(c.analogAprsPttMode));
        q.accept(safe(c.digitalAprsPttMode));
        q.accept(safe(c.aprsReportType));
        q.accept(safe(c.digitalAprsReportChannel));
        q.accept(Long.toString(c.correctFrequencyHz));
        qb.accept(c.smsConfirmation);
        qb.accept(c.excludeFromRoaming);
        q.accept(safe(c.dmrMode));
        qb.accept(c.dataAckDisable);
        q.accept(safe(c.r5ToneBot));
        q.accept(safe(c.r5ToneEot));
        qb.accept(c.autoScan);
        qb.accept(c.anaAprsMute);
        qb.accept(c.sendTalkerAlias);
        q.accept(safe(c.anaAprsTxPath));
        qb.accept(c.arc4);
        q.accept(safe(c.exEmgKind));
        q.accept(safe(c.idle_tx));
        qb.accept(c.compand);
        qb.accept(c.disturEn);
        q.accept(safe(c.disturFreq));
        q.accept(safe(c.rpgaMdc));
        qb.accept(c.dmrCrcIgnore);
        qi.accept(c.txColorCode);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static long parseLongSafe(String[] p, int idx) {
        try {
            return Long.parseLong(stripQuotes(p[idx]));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static int parseIntSafe(String[] p, int idx) {
        try {
            return Integer.parseInt(stripQuotes(p[idx]));
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDoubleSafe(String[] p, int idx) {
        try {
            return Double.parseDouble(stripQuotes(p[idx]));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static boolean parseDigital(String[] p, int idx) {
        try {
            String v = stripQuotes(p[idx]).toLowerCase(java.util.Locale.ROOT);
            return v.startsWith("digi");
        } catch (Exception e) {
            return false;
        }
    }

    private static String fmtFreq(long hz) {
        if (hz <= 0) return "";
        double mhz = hz / 1_000_000.0;
        return String.format(java.util.Locale.ROOT, "%.5f", mhz);
    }

    private static long parseFreq(String s) {
        s = stripQuotes(s).trim();
        if (s.isEmpty()) return 0L;
        s = s.replace("MHz", " ").trim();
        try {
            double mhz = Double.parseDouble(s);
            return Math.round(mhz * 1_000_000.0);
        } catch (Exception e) {
            try {
                return Long.parseLong(s);
            } catch (Exception ex) {
                return 0L;
            }
        }
    }

    private static String powerLabel(int p) {
        switch (p) {
            case 0:
                return "Low";
            case 1:
                return "Mid";
            case 2:
                return "High";
            case 3:
                return "Turbo";
            default:
                return Integer.toString(p);
        }
    }

    private static int parsePower(String[] p, int idx) {
        try {
            String v = stripQuotes(p[idx]).trim().toLowerCase(java.util.Locale.ROOT);
            switch (v) {
                case "low":
                    return 0;
                case "mid":
                    return 1;
                case "medium":
                    return 1;
                case "high":
                    return 2;
                case "turbo":
                    return 3;
                default:
                    return Integer.parseInt(v);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmtBandwidth(double bw) {
        if (bw <= 0) return "";
        if (Math.abs(bw - 12.5) < 0.01) return "12.5K";
        if (Math.abs(bw - 25.0) < 0.01) return "25K";
        return String.format(java.util.Locale.ROOT, "%.1fK", bw);
    }

    private static double parseBandwidth(String s) {
        s = stripQuotes(s).trim().toUpperCase(java.util.Locale.ROOT);
        if (s.endsWith("K")) s = s.substring(0, s.length() - 1);
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String boolLabel(boolean b) {
        return b ? "Yes" : "No";
    }

    private static boolean parseBool(String s) {
        if (s == null) return false;
        s = stripQuotes(s).trim().toLowerCase(java.util.Locale.ROOT);
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String stripQuotes(String s) {
        return s == null ? "" : (s.startsWith("\"") && s.endsWith("\"")) ? s.substring(1, s.length() - 1).replace("\"\"", "\"") : s;
    }

    private static String unesc(String s) {
        return stripQuotes(s);
    }

    private static String[] splitCsv(String l) {
        java.util.List<String> cols = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < l.length(); i++) {
            char ch = l.charAt(i);
            if (ch == '\"') {
                if (q && i + 1 < l.length() && l.charAt(i + 1) == '\"') {
                    cur.append('"');
                    i++;
                } else q = !q;
            } else if (ch == ',' && !q) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }
}

