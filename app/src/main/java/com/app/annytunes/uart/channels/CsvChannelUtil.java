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
import java.util.Locale;

public class CsvChannelUtil {
	public static void write(List<Channel> list, File f) throws IOException {
	    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
	        pw.println("Name,RxHz,TxHz,Digital,ColorCode,Timeslot,ContactId,ContactName,RadioIdIndex,BandwidthKHz,Admit,Power");
	        for (Channel c : list) {
	            pw.printf(Locale.ROOT, "%s,%d,%d,%s,%d,%d,%d,%s,%d,%.1f,%s,%d%n",
	                esc(c.name),
	                c.rxHz,
	                c.txHz,
	                c.digital ? "1" : "0",
	                c.colorCode,
	                c.timeslot,
	                c.contactId,
	                esc(c.contactName),
	                c.radioIdIndex,
	                (c.bandwidthKHz > 0 ? c.bandwidthKHz : 0.0),
	                esc(c.admit),     // "Always" | "CC Free" | "Channel Free"
	                c.power               // 0=Low,1=Med,2=High,3=Turbo
	            );
	        }
	    }
	}

    public static List<Channel> read(File f) throws IOException {
        List<Channel> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line; boolean first=true;
            while ((line = br.readLine()) != null) {
                if (first) { first=false; continue; }
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsv(line);
                if (parts.length < 8) continue; // minimal legacy columns
                Channel c = new Channel();
                c.name        = unesc(parts[0]);
                c.rxHz        = parseLong(parts[1]);
                c.txHz        = parseLong(parts[2]);
                c.digital     = !"0".equals(parts[3]);
                c.colorCode   = parseInt(parts[4]);
                c.timeslot    = parseInt(parts[5]);
                c.contactId   = parseInt(parts[6]);
                c.contactName = unesc(parts[7]);
                if (parts.length > 8) c.radioIdIndex = parseInt(parts[8]);
                if (parts.length > 9) c.bandwidthKHz = parseDouble(parts[9]);
                if (parts.length > 10) c.admit = unesc(parts[10]);
                if (parts.length > 11) c.power = parseInt(parts[11]);
                out.add(c);
            }
        }
        return out;
    }

    public static String header(){ return "Name,RxHz,TxHz,Digital,ColorCode,Timeslot,ContactId,ContactName,RadioIdIndex,BandwidthKHz,Admit,Power"; }
    public static String row(Channel c){ return String.format(java.util.Locale.ROOT, "%s,%d,%d,%s,%d,%d,%d,%s,%d,%.1f,%s,%d",
            esc(c.name), c.rxHz, c.txHz, c.digital?"1":"0", c.colorCode, c.timeslot, c.contactId, esc(c.contactName), c.radioIdIndex,
            (c.bandwidthKHz>0?c.bandwidthKHz:0.0), esc(c.admit), c.power); }

    private static long parseLong(String s){ try { return Long.parseLong(s.trim()); } catch(Exception e){ return 0L; } }
    private static int parseInt(String s){ try { return Integer.parseInt(s.trim()); } catch(Exception e){ return 0; } }
    private static double parseDouble(String s){ try { return Double.parseDouble(s.trim()); } catch(Exception e){ return 0.0; } }

    private static String esc(String s){
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"","\"\"") + "\"";
        return s;
    }
    private static String unesc(String s){
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1).replace("\"\"","\"");
        return s;
    }
    private static String[] splitCsv(String l){
        List<String> cols = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean q=false;
        for (int i=0;i<l.length();i++){
            char ch=l.charAt(i);
            if (ch=='"'){ if (q && i+1<l.length() && l.charAt(i+1)=='"'){cur.append('"'); i++;} else q=!q; }
            else if (ch==',' && !q){ cols.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        cols.add(cur.toString()); return cols.toArray(new String[0]);
    }
}