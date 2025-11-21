package com.app.annytunes.uart.zones;

import com.app.annytunes.uart.channels.ChannelIo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV utility for zones.
 * Format:
 * ZoneName,Channels
 * Where Channels is a list of 1-based channel numbers separated by spaces or semicolons.
 * Example:
 * Local Repeaters,"1 2 3 4 10"
 */
public class ZoneCsvUtil {
    private static final String[] HDR = {"ZoneName", "Channels"};

    public static void write(List<Zone> zones, File f) throws IOException {
        if (zones == null) zones = List.of();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            bw.write(String.join(",", HDR));
            bw.newLine();
            for (Zone z : zones) {
                String name = (z == null || z.name == null) ? "" : z.name.replace('"', ' ');
                StringBuilder chsb = new StringBuilder();
                if (z != null && z.channelNumbers != null) {
                    for (int i = 0; i < z.channelNumbers.length; i++) {
                        if (i > 0) chsb.append(' ');
                        chsb.append(z.channelNumbers[i]);
                    }
                }
                bw.write(name);
                bw.write(',');
                bw.write('"');
                bw.write(chsb.toString());
                bw.write('"');
                bw.newLine();
            }
        }
    }

    public static List<Zone> read(File f) throws IOException {
        List<Zone> out = new ArrayList<>();
        if (f == null || !f.exists()) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) continue;
                // naive CSV split on first comma
                int comma = line.indexOf(',');
                if (comma < 0) continue;
                String name = line.substring(0, comma).trim();
                String rest = line.substring(comma + 1).trim();
                if (rest.startsWith("\"") && rest.endsWith("\""))
                    rest = rest.substring(1, rest.length() - 1);
                String[] toks = rest.split("[ ;,]+");
                List<Integer> chans = new ArrayList<>();
                int maxChannels = Integer.MAX_VALUE;
                try {
                    maxChannels = ChannelIo.getObj().getTotalChannels();
                } catch (Throwable ignored) {
                }
                for (String t : toks) {
                    t = t.trim();
                    if (t.isEmpty()) continue;
                    try {
                        int v = Integer.parseInt(t);
                        if (v >= 1 && v <= maxChannels) chans.add(v); // adjusted to instance method
                    } catch (NumberFormatException ignored) {
                    }
                }
                Zone z = new Zone(name, chans.stream().mapToInt(Integer::intValue).toArray());
                out.add(z);
            }
        }
        return out;
    }
}
