package com.app.annytunes.ui;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.annytunes.uart.zones.Zone;
import com.app.annytunes.uart.zones.ZoneIo;
import com.app.anytunes.R;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ZoneActivity extends AppCompatActivity {
    private static ZoneActivity instance;
    private ListView listZones;
    private Button btnReadZones;
    private Button btnWriteZones; // renamed
    private Button btnSaveZonesCsv;
    private TextView txtStatus;
    private ProgressBar progZones;
    private TextView txtZonePercent;
    private ArrayList<String> rows;
    private ArrayAdapter<String> adapter;
    private ArrayList<Zone> zones;
    private Uri csvUri;
    private int expectedZones;

    public static ZoneActivity getObj() {
        if (instance == null) throw new IllegalStateException("ZoneActivity not initialized");
        return instance;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_zones);
        listZones = findViewById(R.id.listZones);
        btnReadZones = findViewById(R.id.btnReadZones);
        btnWriteZones = findViewById(R.id.btnWriteZones);
        btnSaveZonesCsv = findViewById(R.id.btnSaveZonesCsv);
        txtStatus = findViewById(R.id.txtZoneStatus);
        progZones = findViewById(R.id.progZones);
        txtZonePercent = findViewById(R.id.txtZonePercent);
        zones = new ArrayList<>();
        rows = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                if (position < zones.size() && zones.get(position).name != null && !zones.get(position).name.isEmpty()) {
                    v.setBackgroundColor(Color.TRANSPARENT);
                }
                return v;
            }
        };
        listZones.setAdapter(adapter);
        listZones.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < zones.size()) showEditDialog(zones.get(pos), pos);
        });
        btnReadZones.setOnClickListener(v -> startZoneRead());
        btnWriteZones.setOnClickListener(v -> writeZones());
        btnSaveZonesCsv.setOnClickListener(v -> saveAllZonesCsv());
        expectedZones = com.app.annytunes.uart.zones.ZoneIo.getTotalZones();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    private void startZoneRead() {
        rows.clear();
        zones.clear();
        adapter.notifyDataSetChanged();
        // make progress visible at start
        progZones.setVisibility(android.view.View.VISIBLE);
        txtZonePercent.setVisibility(android.view.View.VISIBLE);
        progZones.setProgress(0);
        txtZonePercent.setText("0%");
        android.util.Log.d("ZoneActivity", "Start reading zone channels then zone names");
        try {
            com.app.annytunes.uart.CommsThread ct = com.app.annytunes.uart.CommsThread.getObj();
            int total = com.app.annytunes.uart.zones.ZoneIo.getTotalZones();
            expectedZones = total;
            ct.setZoneTotalExpected(total); // ensure progress baseline
            for (int i = 1; i <= total; i++) {
                ct.submitZoneChannels(i);
            }
            for (int i = 1; i <= total; i++) {
                ct.submitZoneRead(i);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Zone read start failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onZonesDecoded(List<Zone> decoded, int soFar, int totalExpected) {
        runOnUiThread(() -> {
            if (progZones.getVisibility() != android.view.View.VISIBLE) {
                progZones.setVisibility(android.view.View.VISIBLE);
                txtZonePercent.setVisibility(android.view.View.VISIBLE);
            }
            for (Zone z : decoded) {
                zones.add(z);
                int index = zones.size();
                String displayName = (z.name == null || z.name.isEmpty()) ? ("Zone " + index) : z.name;
                int chCount = (z.channelNumbers == null) ? 0 : z.channelNumbers.length;
                rows.add(String.format(java.util.Locale.getDefault(), "#%d %s (%d ch)", index, displayName, chCount));
            }
            adapter.notifyDataSetChanged();
            txtStatus.setText("Zones: " + zones.size());
            if (totalExpected > 0) {
                int pct = (int) Math.min(100.0, (soFar * 100.0) / totalExpected);
                progZones.setProgress(pct);
                txtZonePercent.setText(pct + "%");
                if (soFar >= totalExpected) {
                    progZones.setVisibility(android.view.View.GONE);
                    txtZonePercent.setVisibility(android.view.View.GONE);
                }
            }
        });
    }

    public void onZoneChannelsDecoded(int zoneIndex) {
        runOnUiThread(() -> {
            if (zoneIndex >= 1 && zoneIndex <= zones.size()) {
                Zone z = zones.get(zoneIndex - 1);
                // refresh channel count in row
                int chCount = (z.channelNumbers == null) ? 0 : z.channelNumbers.length;
                rows.set(zoneIndex - 1, String.format(java.util.Locale.getDefault(), "#%d %s (%d ch)", zoneIndex, (z.name == null || z.name.isEmpty() ? "Zone " + zoneIndex : z.name), chCount));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showEditDialog(Zone z, int position) {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Edit Zone #" + (position + 1));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        EditText name = new EditText(this);
        name.setHint("Zone Name");
        name.setText(z.name);
        EditText chans = new EditText(this);
        chans.setHint("Channels (space separated)");
        StringBuilder sb = new StringBuilder();
        if (z.channelNumbers != null) {
            for (int i = 0; i < z.channelNumbers.length; i++) {
                if (i > 0) sb.append(' ');
                sb.append(z.channelNumbers[i]);
            }
        }
        chans.setText(sb.toString());
        layout.addView(name);
        layout.addView(chans);
        b.setView(layout);
        b.setPositiveButton("Save", (d, w) -> {
            z.name = name.getText().toString().trim();
            String[] toks = chans.getText().toString().trim().split("[ ,;]+");
            ArrayList<Integer> list = new ArrayList<>();
            for (String t : toks) {
                if (t.isEmpty()) continue;
                try {
                    int v = Integer.parseInt(t);
                    if (v >= 1 && v <= ZoneIo.getTotalZones()) list.add(v);
                } catch (Exception ignored) {
                }
            }
            z.channelNumbers = list.stream().mapToInt(Integer::intValue).toArray();
            rows.set(position, String.format(java.util.Locale.getDefault(), "#%d %s (%d ch)", position + 1, (z.name == null || z.name.isEmpty() ? "Zone " + (position + 1) : z.name), z.channelNumbers == null ? 0 : z.channelNumbers.length));
            adapter.notifyDataSetChanged();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void writeZones() {
        if (zones == null || zones.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "No zones to write", Toast.LENGTH_SHORT).show());
            return;
        }
        new Thread(() -> {
            try {
                com.app.annytunes.uart.CommsThread comms = com.app.annytunes.uart.CommsThread.getObj();
                for (int i = 0; i < zones.size(); i++) {
                    com.app.annytunes.uart.zones.Zone z = zones.get(i);
                    long addr = com.app.annytunes.uart.zones.ZoneIo.addressOfZone(i + 1);
                    byte[] rec = com.app.annytunes.uart.zones.ZoneIo.encodeZone(z, com.app.annytunes.uart.zones.ZoneIo.DEFAULT_ZONE_RECORD_SIZE);
                    try {
                        comms.submitWrite(addr, rec);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted zone write", ie);
                    }
                }
                try {
                    comms.commitAndKeepAlive();
                } catch (Exception ce) {
                    throw new RuntimeException("Commit failed", ce);
                }
                runOnUiThread(() -> Toast.makeText(this, "Zones written", Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                runOnUiThread(() -> Toast.makeText(this, "Zone write failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveAllZonesCsv() {
        if (csvUri == null) {
            Toast.makeText(this, "No CSV dest", Toast.LENGTH_SHORT).show();
            return;
        }
        try (var os = getContentResolver().openOutputStream(csvUri, "w"); var pw = new PrintWriter(new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("ZoneName,Channels");
            for (Zone z : zones) {
                String name = z.name == null ? "" : z.name.replace('"', ' ');
                StringBuilder chsb = new StringBuilder();
                if (z.channelNumbers != null) {
                    for (int i = 0; i < z.channelNumbers.length; i++) {
                        if (i > 0) chsb.append(' ');
                        chsb.append(z.channelNumbers[i]);
                    }
                }
                pw.print(name);
                pw.print(',');
                pw.print('"');
                pw.print(chsb);
                pw.println('"');
            }
            Toast.makeText(this, "Zones CSV saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "CSV save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
