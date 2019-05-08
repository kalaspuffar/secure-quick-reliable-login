package org.ea.sqrl.activites.base;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
}
