package com.app.annytunes.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.app.anytunes.R;

/**
 * Dialog displaying information about Annytunes, Git repository,
 * and contributors / contributions.
 */
public class AboutDialog {

    public static final String GIT_URL = "https://github.com/dirtybug/annytunes";
    public static final String CONTRIBUTORS_URL = "https://github.com/dirtybug/annytunes/graphs/contributors";

    public static void openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void show(Context context) {
        String versionName = "1.3.0";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (pInfo.versionName != null) {
                versionName = pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        String message = "<b>Annytunes</b> v" + versionName + "<br/><br/>"
                + "Open-source AnyTone AT-D878UV / DMR Codeplug Channel &amp; Zone Manager for Android.<br/><br/>"
                + "<b>Git Repository:</b><br/>"
                + "<a href=\"" + GIT_URL + "\">" + GIT_URL + "</a><br/><br/>"
                + "<b>Contributors &amp; Credits:</b><br/>"
                + "• <b>dirtybug</b> — Project founder &amp; DMR UART protocol<br/>"
                + "• <b>J. Andrade</b> — Docker CI/CD, tests &amp; release automation<br/>"
                + "• <b>jcalado</b> — Material 3 theme &amp; Flutter CSV services<br/>"
                + "• <b>felHR85</b> — UsbSerial Android driver<br/><br/>"
                + "<b>Contributions:</b><br/>"
                + "Contributions are welcome! Submit pull requests, report issues, or suggest new radio features on GitHub.";

        TextView textView = new TextView(context);
        textView.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        int padding = (int) (18 * context.getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
        textView.setTextSize(14f);

        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setView(textView)
                .setPositiveButton(R.string.about_open_git, (dialog, which) -> openUrl(context, GIT_URL))
                .setNeutralButton(R.string.about_view_contributions, (dialog, which) -> openUrl(context, CONTRIBUTORS_URL))
                .setNegativeButton(R.string.about_close, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
