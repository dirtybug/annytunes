package com.app.annytunes.ui;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.annytunes.uart.AnytoneUart;
import com.app.annytunes.uart.CommsThread;
import com.app.annytunes.uart.channels.Channel;
import com.app.annytunes.uart.channels.ChannelIo;
import com.app.annytunes.uart.channels.CsvChannelUtil;
import com.app.anytunes.R;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChannelTransferActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTOR = "selector";
    private static final String DEFAULT_CSV_NAME = "channels.csv";

    private static ChannelTransferActivity instance;

    private TextView statusText;
    private Button btnDownload;
    private Button btnUpload;
    private Button btnRefresh;
    private Button btnEnterPcMode; // new button reference
    private ListView listChannels;
    private ProgressBar progressRead;
    private TextView txtProgressPercent;
    private Button btnSaveAll;
    private Button btnSaveChanges;
    private static volatile boolean channelsLoaded; // set true after full channel read completes
    private Button btnCommitExit;

    private String selector;
    private AnytoneUart uart;

    private ArrayList<String> rows;
    private android.widget.ArrayAdapter<String> adapter;
    private ArrayList<Channel> channels; // track real channel objects for editing
    private File currentCsvFile; // last chosen CSV destination
    private ArrayList<Channel> originalChannels; // snapshot for change comparison
    private Uri chosenCsvUri; // user-selected CSV document
    private Button btnExitNoCommit;

    private final ActivityResultLauncher<String> createDocLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"), this::onCreateCsvDestination);

    private final ActivityResultLauncher<String[]> openDocLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onCsvPicked);

    public static ChannelTransferActivity getObj() {
        if (instance == null) throw new IllegalStateException("ChannelTransferActivity not initialized yet");
        return instance;
    }

    public static boolean areChannelsLoaded() {
        return channelsLoaded;
    }

    public static int getLoadedChannelCount() {
        try {
            ChannelTransferActivity inst = getObj();
            return inst.channels == null ? 0 : inst.channels.size();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String getChannelName(int channelNumber) {
        try {
            ChannelTransferActivity inst = getObj();
            if (channelNumber < 1 || channelNumber > inst.channels.size()) return "";
            Channel c = inst.channels.get(channelNumber - 1);
            return (c.name == null ? "" : c.name);
        } catch (Throwable e) {
            return "";
        }
    }

    private File getLastCsvFile() {
        return new File(getFilesDir(), DEFAULT_CSV_NAME); // internal storage persistent
    }

    private void connectSerial() {
        try {
            uart = new AnytoneUart(this, selector);
            CommsThread.getObj().enterPcMode();
            new ChannelIo(); // initialize ChannelIo singleton from AnytoneUart.getObj()
            statusText.setText("Connected: " + selector);
            // Removed autoLoadLastCsv to avoid unintended writes on connect
        } catch (Exception e) {
            statusText.setText("Connect failed: " + e.getMessage());
            Toast.makeText(this, "Connect error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testHandshake() {
        if (uart == null) return;
        try {
            CommsThread.getObj().handshake();
            Toast.makeText(this, "Handshake OK", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Handshake failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startDownloadFlow() {
        createDocLauncher.launch(DEFAULT_CSV_NAME);
    }

    private void onCreateCsvDestination(Uri uri) {
        if (uri == null) return;
        if (uart == null) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        progressRead.setIndeterminate(false);
        progressRead.setProgress(0);
        progressRead.setMax(100);
        progressRead.setVisibility(android.view.View.VISIBLE);
        txtProgressPercent.setVisibility(android.view.View.VISIBLE);
        txtProgressPercent.setText("0%");
        rows = new ArrayList<>();
        adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (position < channels.size() && channels.get(position).edited) {
                    v.setBackgroundColor(Color.YELLOW);
                } else {
                    v.setBackgroundColor(Color.TRANSPARENT);
                }
                return v;
            }
        };
        listChannels.setAdapter(adapter);
        channels.clear();
        chosenCsvUri = uri;
        currentCsvFile = null; // switch to Uri-based persistence

        // Direct call; no background thread needed since this only enqueues tasks
        try {
            ChannelIo.getObj().readAllChannelsAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void onError () {

    }
    private void onEnd () {
        progressRead.setVisibility(View.GONE);
        txtProgressPercent.setVisibility(View.GONE);
        Toast.makeText(this, "Read complete: " + channels.size() + " channels", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        instance = this;
        selector = getIntent().getStringExtra(EXTRA_SELECTOR);
        statusText = findViewById(R.id.txtStatus);
        btnDownload = findViewById(R.id.btnDownload);
        btnUpload = findViewById(R.id.btnUpload);
        btnRefresh = findViewById(R.id.btnRefreshChannels);
        btnEnterPcMode = findViewById(R.id.btnEnterPcMode);
        listChannels = findViewById(R.id.listChannels);
        progressRead = findViewById(R.id.progressRead);
        txtProgressPercent = findViewById(R.id.txtProgressPercent);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCommitExit = findViewById(R.id.btnCommitExit);
        btnExitNoCommit = findViewById(R.id.btnExitNoCommit);

        btnDownload.setOnClickListener(v -> startDownloadFlow());
        btnUpload.setOnClickListener(v -> startUploadFlow());
        btnRefresh.setOnClickListener(v -> testHandshake());
        btnSaveAll.setOnClickListener(v -> saveAllCsv());
        btnSaveChanges.setOnClickListener(v -> writeEditedChannels());
        btnEnterPcMode.setOnClickListener(v -> enterPcModeAction());
        btnCommitExit.setOnClickListener(v -> doCommitAndExit());
        btnExitNoCommit.setOnClickListener(v -> exitWithoutCommit());

        connectSerial();

        channels = new ArrayList<>();
        originalChannels = new ArrayList<>();
        listChannels.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < channels.size()) ChannelEditDialog.show(this, channels.get(position), position);
        });
        applyInitialButtonState();
    }

    private void startUploadFlow() {
        openDocLauncher.launch(new String[]{"text/csv","application/octet-stream"});
    }

    private void onCsvPicked(Uri uri) {
        if (uri == null) return;
        if (uart == null) { Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show(); return; }
        btnUpload.setEnabled(false);
        new Thread(() -> {
            try {
                File tmp = new File(getCacheDir(), "import.csv");
                try (var is = getContentResolver().openInputStream(uri); var fos = new java.io.FileOutputStream(tmp)) {
                    if (is == null) throw new IOException("openInputStream returned null");
                    byte[] buf = new byte[8192]; int r; while ((r = is.read(buf)) > 0) fos.write(buf, 0, r);
                }
                List<Channel> chans = CsvChannelUtil.read(tmp);
                ChannelIo.getObj().writeAllChannels(chans); // this now commits internally
                runOnUiThread(() -> {
                    Toast.makeText(this, "Uploaded " + chans.size() + " channels", Toast.LENGTH_LONG).show();
                    enableCommitPending();
                });
            } catch (Exception e) {
                runOnUiThread(() -> { Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); btnUpload.setEnabled(true); });
            }
        }).start();
    }

    private void saveAllCsv() {
        if (channels == null || channels.isEmpty()) { Toast.makeText(this,"No channels",Toast.LENGTH_SHORT).show(); return; }
        if (chosenCsvUri == null) { Toast.makeText(this,"No CSV destination",Toast.LENGTH_SHORT).show(); return; }
        try (var os = getContentResolver().openOutputStream(chosenCsvUri, "w"); var pw = new PrintWriter(new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println(CsvChannelUtil.header());
            for (Channel c: channels) pw.println(CsvChannelUtil.row(c));
            Toast.makeText(this,"Saved all to CSV",Toast.LENGTH_SHORT).show();
        } catch(Exception e){ Toast.makeText(this,"Save all failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private void writeEditedChannels() {
        if (channels == null || channels.isEmpty()) { Toast.makeText(this,"No channels",Toast.LENGTH_SHORT).show(); return; }
        ArrayList<Integer> editedIndices = new ArrayList<>();
        for (int i=0;i<channels.size();i++) if (channels.get(i).edited) editedIndices.add(i);
        if (editedIndices.isEmpty()) { Toast.makeText(this,"No edited channels",Toast.LENGTH_SHORT).show(); return; }
        btnSaveChanges.setEnabled(false);
        new Thread(() -> {
            try {
                ChannelIo cio = ChannelIo.getObj();
                com.app.annytunes.uart.CommsThread comms = com.app.annytunes.uart.CommsThread.getObj();
                for (int idx : editedIndices) {
                    Channel c = channels.get(idx);
                    long addr = cio.channelIndexToAddress(idx + 1);
                    if (addr < 0) continue;
                    byte[] rec = cio.encodeChannel(c, ChannelIo.CH_OFFSET);
                    try { comms.submitWrite(addr, rec); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new IOException("Interrupted submitting write", ie); }
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Queued " + editedIndices.size() + " edited channel(s); commit pending", Toast.LENGTH_LONG).show();
                    btnSaveChanges.setEnabled(true);
                    btnCommitExit.setEnabled(true);
                    enableCommitPending();
                });
            } catch(Exception e){ runOnUiThread(() -> { Toast.makeText(this,"Write edited failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); btnSaveChanges.setEnabled(true); }); }
        }).start();
    }
    private void navigateHome() {
        try {
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Throwable t) { /* ignore */ }
    }

    public void updateRow(int position) {
        if (rows == null || channels == null) return;
        if (position < 0 || position >= channels.size() || position >= rows.size()) return;
        Channel c = channels.get(position);
        String mode = c.digital ? "Digital" : "Analog";
        String name = (c.name == null || c.name.isEmpty()) ? "<empty>" : c.name;
        rows.set(position, String.format(Locale.getDefault(), "#%d  %s  (%s)%s", position + 1, name, mode, c.edited ? " *" : ""));
        if (adapter != null) adapter.notifyDataSetChanged();
    }
    public void persistChannelEdit(int position) {
        updateRow(position);
        if (chosenCsvUri == null) return; // no destination yet
        try (var os = getContentResolver().openOutputStream(chosenCsvUri, "w"); var pw = new PrintWriter(new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println(CsvChannelUtil.header());
            for (Channel c : channels) pw.println(CsvChannelUtil.row(c));
        } catch (Exception e) {
            Toast.makeText(this, "CSV save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enterPcModeAction() {
        try {
            CommsThread.getObj().enterPcMode();
            Toast.makeText(this, "PC Mode entered", Toast.LENGTH_SHORT).show();
            applyPcModeActiveState();
        } catch (Exception e) {
            Toast.makeText(this, "Enter PC failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyInitialButtonState() {
        btnEnterPcMode.setEnabled(false); // disabled at start
        btnDownload.setEnabled(true);
        btnUpload.setEnabled(true);
        btnRefresh.setEnabled(true);
        btnSaveAll.setEnabled(true);
        btnSaveChanges.setEnabled(true);
        btnCommitExit.setEnabled(false); // enable only after writes queued
    }

    public void applyAfterWriteState() {
        // Only allow re-enter PC mode; all other actions disabled
        btnEnterPcMode.setEnabled(true);
        btnDownload.setEnabled(false);
        btnUpload.setEnabled(false);
        btnRefresh.setEnabled(false);
        btnSaveAll.setEnabled(false);
        btnSaveChanges.setEnabled(true);
        btnCommitExit.setEnabled(true); // enable only after writes queued
    }
    private void applyPcModeActiveState() {
        // PC mode engaged; disable enter button, enable others
        btnEnterPcMode.setEnabled(false);
        btnDownload.setEnabled(true);
        btnUpload.setEnabled(true);
        btnRefresh.setEnabled(true);
        btnSaveAll.setEnabled(true);
        btnSaveChanges.setEnabled(true);
    }

    public void onEnterPcMode(boolean ok, String message){
        runOnUiThread(() -> {
            if (ok) {
                Toast.makeText(this, "PC Mode entered", Toast.LENGTH_SHORT).show();
                applyPcModeActiveState();
            } else {
                Toast.makeText(this, "PC Mode failed: " + message, Toast.LENGTH_LONG).show();
                // keep button enabled to retry
                btnEnterPcMode.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1, 0, "Zones");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new android.content.Intent(this, ZoneActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Method pointer target for CommsThread observer
    public void onChannelsDecoded(List<Channel> chunk, int soFar, int totalExpected) {
        runOnUiThread(() -> {
            for (Channel c : chunk) {
                channels.add(c);
                String mode = c.digital ? "Digital" : "Analog";
                int number = rows.size() + 1;
                String name = (c.name == null || c.name.isEmpty()) ? "<empty>" : c.name;
                rows.add(String.format(Locale.getDefault(), "#%d  %s  (%s)", number, name, mode));
            }
            adapter.notifyDataSetChanged();
            int pct = (totalExpected > 0) ? (soFar * 100 / totalExpected) : 0;
            progressRead.setProgress(pct);
            txtProgressPercent.setText(pct + "%");
            if (totalExpected > 0 && soFar >= totalExpected) {
                originalChannels.clear();
                originalChannels.addAll(channels); // snapshot after initial load
                channelsLoaded = true;
                // notify ZoneActivity if open
                try {
                    ZoneActivity.getObj().onChannelsReady();
                } catch (Throwable ignored) {
                }
                onEnd();
            }
        });
    }

    private void doCommitAndExit() {
        // Manual commit of queued channel/zone writes, then exit
        if (btnCommitExit != null) btnCommitExit.setEnabled(false);
        new Thread(() -> {
            try {
                CommsThread.getObj().commitWriteSync();

                runOnUiThread(this::navigateHome);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Commit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (btnCommitExit != null) btnCommitExit.setEnabled(true);
                });
            }
        }).start();
    }

    private void exitWithoutCommit() {
        navigateHome();
    }

    public void enableCommitPending() {
        runOnUiThread(() -> {
            if (btnCommitExit != null) btnCommitExit.setEnabled(true);
        });


    }
}
