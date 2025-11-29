package com.app.annytunes.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.app.annytunes.uart.channels.Channel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// New class to handle fetching and comparing channels
public class FetchAndCompareChannelsTask {

    private final Context context;
    private final List<Channel> currentChannels;

    public FetchAndCompareChannelsTask(Context context, List<Channel> currentChannels) {
        this.context = context;
        this.currentChannels = currentChannels;
    }

    public void execute() {
        // Fetch CSV data from GitHub
        new Thread(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/jcalado/repetidores/master/data/anacon.csv");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    List<String> csvChannels = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        csvChannels.add(line);
                    }
                    reader.close();

                    // Compare channels
                    List<String> missingChannels = findMissingChannels(csvChannels);

                    // Show missing channels in UI
                    new Handler(Looper.getMainLooper()).post(() -> showMissingChannelsDialog(missingChannels));
                }
            } catch (Exception e) {
                Log.e("FetchAndCompareChannelsTask", "Error fetching channels", e);
            }
        }).start();
    }

    // Updated to use the `name` field directly instead of a non-existent `getName` method
    private List<String> findMissingChannels(List<String> csvChannels) {
        List<String> missingChannels = new ArrayList<>();
        for (String csvChannel : csvChannels) {
            boolean found = false;
            for (Channel currentChannel : currentChannels) {
                if (currentChannel.name.equalsIgnoreCase(csvChannel)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingChannels.add(csvChannel);
            }
        }
        return missingChannels;
    }

    private void showMissingChannelsDialog(List<String> missingChannels) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Missing Channels");
        builder.setMessage(TextUtils.join("\n", missingChannels));
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}

