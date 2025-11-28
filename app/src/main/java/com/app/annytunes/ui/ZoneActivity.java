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
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
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
    private final java.util.HashMap<Integer, com.app.annytunes.uart.zones.Zone> originalZones = new java.util.HashMap<>(); // index->snapshot

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
                if (position >= 0 && position < zones.size()) {
                    Zone z = zones.get(position);
                    int chCount = (z.channelNumbers == null) ? 0 : z.channelNumbers.length;
                    String nameStr = (z.name == null || z.name.isEmpty() || ZoneIo.isZoneEmpty(z.name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)))
                            ? ("Zone " + (position + 1)) : z.name;
                    String countStr = "(" + chCount + " ch)";
                    String label = "#" + (position + 1) + " " + nameStr + " " + countStr;
                    android.text.SpannableString ss = new android.text.SpannableString(label);

                    if (z.changedChannels) {
                        int cs = label.lastIndexOf(countStr);
                        if (cs >= 0)
                            ss.setSpan(new android.text.style.BackgroundColorSpan(Color.YELLOW), cs, cs + countStr.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (v instanceof TextView) ((TextView) v).setText(ss);
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
        btnReadZones.setEnabled(ChannelTransferActivity.areChannelsLoaded());
        btnWriteZones.setEnabled(ChannelTransferActivity.areChannelsLoaded());
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

    public void progress(int soFar, int totalExpected) {
        runOnUiThread(() -> {
            if (progZones.getVisibility() != android.view.View.VISIBLE) {
                progZones.setVisibility(android.view.View.VISIBLE);
                txtZonePercent.setVisibility(android.view.View.VISIBLE);
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
    public void onZonesDecoded(List<Zone> decoded, int soFar, int totalExpected) {
        runOnUiThread(() -> {
            if (progZones.getVisibility() != android.view.View.VISIBLE) {
                progZones.setVisibility(android.view.View.VISIBLE);
                txtZonePercent.setVisibility(android.view.View.VISIBLE);
            }
            for (Zone z : decoded) {
                zones.add(z);
                int index = zones.size();
                // snapshot original for change detection
                Zone snap = new Zone(z.name, z.channelNumbers);
                originalZones.put(index - 1, snap);
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
        b.setTitle("Zone #" + (position + 1));
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        EditText name = new EditText(this);
        name.setHint("Zone Name");
        name.setText(z.name);
        layout.addView(name);
        // Table of current channels
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        TableRow header = new TableRow(this);
        addCell(header, "#");
        addCell(header, "Channel");
        addCell(header, "Name");
        addCell(header, "Remove");
        table.addView(header);
        int[] nums = z.channelNumbers == null ? new int[0] : z.channelNumbers;
        java.util.LinkedHashSet<Integer> current = new java.util.LinkedHashSet<>();
        for (int v : nums) current.add(v);
        for (int i = 0; i < nums.length; i++) {
            int chNum = nums[i];
            TableRow tr = new TableRow(this);
            addCell(tr, Integer.toString(i + 1));
            addCell(tr, Integer.toString(chNum));
            addCell(tr, ChannelTransferActivity.getChannelName(chNum));
            Button rm = new Button(this);
            rm.setText("X");
            rm.setOnClickListener(v -> {
                current.remove(chNum);
                rebuildTable(table, current);
            });
            tr.addView(rm);
            table.addView(tr);
        }
        layout.addView(table);
        // Add channel section
        Button btnAdd = new Button(this);
        btnAdd.setText("Add Channel");
        layout.addView(btnAdd);
        TextView txtAddInfo = new TextView(this);
        layout.addView(txtAddInfo);
        btnAdd.setOnClickListener(v -> {
            int max = ChannelTransferActivity.getLoadedChannelCount();
            // Build list of available channels not in current
            java.util.ArrayList<Integer> available = new java.util.ArrayList<>();
            for (int i = 1; i <= max; i++) {
                if (!current.contains(i) && current.size() < 250) available.add(i);
            }
            if (available.isEmpty()) {
                Toast.makeText(this, "No channels left (max 250 or all used)", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] labels = new String[available.size()];
            for (int i = 0; i < available.size(); i++) {
                int ch = available.get(i);
                labels[i] = ch + " - " + ChannelTransferActivity.getChannelName(ch);
            }
            android.app.AlertDialog.Builder sel = new android.app.AlertDialog.Builder(this);
            sel.setTitle("Select Channel to Add");
            sel.setItems(labels, (dialog, which) -> {
                int ch = available.get(which);
                current.add(ch);
                rebuildTable(table, current);
            });
            sel.setNegativeButton("Cancel", null);
            sel.show();
        });
        scroll.addView(layout);
        b.setView(scroll);
        b.setPositiveButton("Save", (d, w) -> {
            String oldName = z.name == null ? "" : z.name;
            int[] oldNums = z.channelNumbers == null ? new int[0] : z.channelNumbers.clone();
            String newName = name.getText().toString().trim();
            if (newName.length() > 31)
                newName = newName.substring(0, 31); // clamp to 31 (leave null terminator)
            z.name = newName;
            z.channelNumbers = current.stream().mapToInt(Integer::intValue).toArray();
            // flags: name or membership changed
            Zone.changedName = !oldName.equals(z.name);
            z.changedChannels = !java.util.Arrays.equals(oldNums, z.channelNumbers);
            // Update row text
            String label = String.format(java.util.Locale.getDefault(), "#%d %s (%d ch)", position + 1, (z.name == null || z.name.isEmpty() ? "Zone " + (position + 1) : z.name), z.channelNumbers.length);
            rows.set(position, label);
            adapter.notifyDataSetChanged();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void rebuildTable(TableLayout table, java.util.LinkedHashSet<Integer> current) {
        table.removeAllViews();
        TableRow header = new TableRow(this);
        addCell(header, "#");
        addCell(header, "Channel");
        addCell(header, "Name");
        addCell(header, "Remove");
        table.addView(header);
        int idx = 0;
        for (int ch : current) {
            TableRow tr = new TableRow(this);
            addCell(tr, Integer.toString(++idx));
            addCell(tr, Integer.toString(ch));
            addCell(tr, ChannelTransferActivity.getChannelName(ch));
            Button rm = new Button(this);
            rm.setText("X");
            rm.setOnClickListener(v -> {
                current.remove(ch);
                rebuildTable(table, current);
            });
            tr.addView(rm);
            table.addView(tr);
        }
    }

    private void addCell(TableRow tr, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 4, 8, 4);
        tr.addView(tv);
    }

    private void writeZones() {
        if (zones == null || zones.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "No zones to write", Toast.LENGTH_SHORT).show());
            return;
        }
        new Thread(() -> {
            try {

                ZoneIo.writeAllZones(zones);
                runOnUiThread(() -> Toast.makeText(this, "Zones written successfully.", Toast.LENGTH_LONG).show());
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

    public void onChannelsReady() {
        runOnUiThread(() -> {
            if (btnReadZones != null) btnReadZones.setEnabled(true);
            if (btnWriteZones != null) btnWriteZones.setEnabled(true);
        });
    }
}
