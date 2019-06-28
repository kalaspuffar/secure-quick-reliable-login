package org.ea.sqrl.activites.base;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import org.ea.sqrl.R;
import org.ea.sqrl.utils.Utils;

/**
 * This activity is inherited by all other activities and provides common logic
 * such as language support.
 *
 * @author Alexander Hauser
 */
public class CommonBaseActivity extends AppCompatActivity {
    private static final String TAG = "CommonBaseActivity";
    protected  String mCurrentLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setLanguage(this);
        Utils.reloadActivityTitle(this);

        mCurrentLanguage = Utils.getLanguage(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        recreateIfLanguageChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreateIfLanguageChanged();
    }

    protected void recreateIfLanguageChanged() {
        String language = Utils.getLanguage(this);

        if (!mCurrentLanguage.equals(language)) {
            mCurrentLanguage = language;
            this.recreate();
        }
    }

    protected void showInfoMessage(String title, String message, Runnable done) {
        if (message == null) return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        if (title != null) alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setIcon(R.drawable.ic_info_accent_24dp)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> done.run())
                .create()
                .show();
    }

    protected void showInfoMessage(@StringRes int title, @StringRes int message) {
        showInfoMessage(
                this.getResources().getString(title),
                this.getResources().getString(message),
                () -> {}
        );
    }

    protected void showInfoMessage(@StringRes int title, @StringRes int message, Runnable done) {
        showInfoMessage(
                this.getResources().getString(title),
                this.getResources().getString(message),
                done
        );
    }
}
