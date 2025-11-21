package com.app.annytunes.ui;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.anytunes.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.app.annytunes.USB_PERMISSION";
    private Spinner deviceSpinner;
    private Button refreshBtn;
    private Button connectBtn;
    private UsbManager usbManager;
    private final List<UsbDevice> enumeratedDevices = new ArrayList<>();
    private PendingIntent usbPermissionIntent;
    private UsbDevice pendingDeviceForPermission;

    private static MainActivity instance;

    private final ActivityResultLauncher<String> storagePermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, "Storage permission denied; CSV import/export may fail", Toast.LENGTH_LONG).show();
                }
            }
    );

    private void registerUsbPermissionReceiver() {
        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, usbFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            //noinspection deprecation
            registerReceiver(usbPermissionReceiver, usbFilter);
        }
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) return;
            UsbDevice device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
            } else {
                //noinspection deprecation
                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            }
            if (device == null) return;
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted) {
                launchTransferForDevice(device);
            } else {
                Toast.makeText(context, "USB permission denied", Toast.LENGTH_LONG).show();
            }
            pendingDeviceForPermission = null;
        }
    };

    public static MainActivity getObj() {
        if (instance == null) throw new IllegalStateException("MainActivity not ready");
        return instance;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        deviceSpinner = findViewById(R.id.spinnerDevices);
        refreshBtn = findViewById(R.id.btnRefresh);
        connectBtn = findViewById(R.id.btnConnect);

        usbPermissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );
        registerUsbPermissionReceiver();

        refreshBtn.setOnClickListener(v -> enumerateDevices());
        connectBtn.setOnClickListener(this::onConnectClicked);

        enumerateDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void enumerateDevices() {
        Map<String, UsbDevice> map = usbManager.getDeviceList();
        enumeratedDevices.clear();
        enumeratedDevices.addAll(new LinkedHashMap<>(map).values());
        List<String> labels = new ArrayList<>();
        for (UsbDevice d : enumeratedDevices) {
            labels.add(String.format("VID_%04X PID_%04X %s", d.getVendorId(), d.getProductId(), d.getDeviceName()));
        }
        if (labels.isEmpty()) labels.add("<no usb devices>");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);
    }

    private void onConnectClicked(View v) {
        if (enumeratedDevices.isEmpty()) {
            Toast.makeText(this, "No USB devices", Toast.LENGTH_SHORT).show();
            return;
        }
        int idx = deviceSpinner.getSelectedItemPosition();
        if (idx < 0 || idx >= enumeratedDevices.size()) {
            Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show();
            return;
        }
        UsbDevice dev = enumeratedDevices.get(idx);
        if (usbManager.hasPermission(dev)) {
            launchTransferForDevice(dev);
        } else {
            pendingDeviceForPermission = dev;
            usbManager.requestPermission(dev, usbPermissionIntent);
            Toast.makeText(this, "Requesting USB permission…", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchTransferForDevice(UsbDevice dev) {
        String selector = String.format("%04x:%04x", dev.getVendorId(), dev.getProductId());
        Intent intent = new Intent(this, ChannelTransferActivity.class);
        intent.putExtra(ChannelTransferActivity.EXTRA_SELECTOR, selector);
        startActivity(intent);
    }

    // Callback for PC mode enter result
    public void onEnterPcMode(boolean ok, String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, ok ? "PC Mode OK" : "PC Mode failed: " + message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
