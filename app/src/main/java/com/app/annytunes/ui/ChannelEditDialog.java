package com.app.annytunes.ui;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.app.annytunes.uart.channels.Channel;

import java.util.Locale;

/** Dialog helper for editing a single Channel. */
public final class ChannelEditDialog {
    private ChannelEditDialog() {}

    public static void show(ChannelTransferActivity act, Channel ch, int position) {
        if (act == null || ch == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(act);
        b.setTitle("Edit Channel #" + (position + 1));
        LinearLayout layout = new LinearLayout(act);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (8 * act.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        EditText name = new EditText(act); name.setHint("Name"); name.setText(ch.name);
        EditText rx = new EditText(act); rx.setHint("RX MHz"); rx.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); rx.setText(hzToMHz(ch.rxHz));
        EditText tx = new EditText(act); tx.setHint("TX MHz"); tx.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); tx.setText(hzToMHz(ch.txHz));
        Spinner mode = new Spinner(act);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, new String[]{"Digital","Analog"});
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setAdapter(modeAdapter); mode.setSelection(ch.digital ? 0 : 1);
        Spinner ccSpin = new Spinner(act);
        String[] ccVals = new String[16]; for(int i=0;i<16;i++) ccVals[i]=String.valueOf(i);
        ArrayAdapter<String> ccAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, ccVals);
        ccAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ccSpin.setAdapter(ccAdapter); ccSpin.setSelection(Math.max(0, Math.min(15, ch.colorCode)));
        Spinner tsSpin = new Spinner(act);
        ArrayAdapter<String> tsAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, new String[]{"1","2"});
        tsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tsSpin.setAdapter(tsAdapter); tsSpin.setSelection(ch.timeslot == 1 ? 0 : 1);
        Spinner admitSpin = new Spinner(act);
        String[] admitOpts = new String[]{"Always","CC Free","Channel Free"};
        ArrayAdapter<String> admitAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, admitOpts);
        admitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        admitSpin.setAdapter(admitAdapter);
        int admitIdx = 0; if ("CC Free".equalsIgnoreCase(ch.admit)) admitIdx = 1; else if ("Channel Free".equalsIgnoreCase(ch.admit)) admitIdx = 2; admitSpin.setSelection(admitIdx);
        Spinner powSpin = new Spinner(act);
        String[] powerOpts = new String[]{"Low","Med","High","Turbo"};
        ArrayAdapter<String> powAdapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_item, powerOpts);
        powAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        powSpin.setAdapter(powAdapter); powSpin.setSelection(Math.max(0, Math.min(3, ch.power)));
        layout.addView(name); layout.addView(rx); layout.addView(tx); layout.addView(mode); layout.addView(ccSpin); layout.addView(tsSpin); layout.addView(admitSpin); layout.addView(powSpin);
        b.setView(layout);
        b.setPositiveButton("Save", (d, w) -> {
            ch.name = safeStr(name.getText());
            ch.rxHz = mhzToHz(rx.getText());
            ch.txHz = mhzToHz(tx.getText());
            ch.digital = (mode.getSelectedItemPosition()==0);
            ch.colorCode = ccSpin.getSelectedItemPosition();
            ch.timeslot = tsSpin.getSelectedItemPosition()==0 ? 1 : 2;
            ch.admit = admitSpin.getSelectedItem().toString();
            ch.power = powSpin.getSelectedItemPosition();
            ch.edited = true;
            act.persistChannelEdit(position);
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private static String hzToMHz(long hz) { if (hz <= 0) return ""; double mhz = hz / 1_000_000.0; return String.format(Locale.getDefault(), "%.6f", mhz); }
    private static long mhzToHz(CharSequence cs) { try { double v = Double.parseDouble(cs.toString().trim()); return (long)(v * 1_000_000L); } catch(Exception e){ return 0L; } }
    private static String safeStr(CharSequence cs){ return cs==null?"":cs.toString().trim(); }
}
