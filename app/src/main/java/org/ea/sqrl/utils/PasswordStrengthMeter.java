package org.ea.sqrl.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Calculates and displays a password strength rating.
 * Expects the password_strength_meter.xml to be present in the calling layout
 *
 * @author Alexander Hauser (alexhauser)
 */
public class PasswordStrengthMeter {

    private final int PW_MIN_LENGTH = 8;
    private final int STRENGTH_POINTS_MIN_MEDIUM = 5;
    private final int STRENGTH_POINTS_MIN_GOOD = 9;
    private final Context mContext;
    private ViewGroup mPassStrengthLayout;
    private CalcPasswordStrengthAsyncTask mLastCalcTask;


    /**
     * Creates a PasswordStrengthMeter object.
     *
     * @param context The context of the caller.
     */
    public PasswordStrengthMeter(Context context) {

        mContext = context;
    }

    /**
     * Registers a TextChangedListener for the given password field and calculates
     * and displays the password strength each time the listener fires.
     *
     * @param passwordField The password field to be observed.
     * @param passStrengthLayout A ViewGroup containing the password_strength_meter.xml layout.
     */
    public void register(EditText passwordField, ViewGroup passStrengthLayout) {

        mPassStrengthLayout = passStrengthLayout;
        passwordField.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String password = s.toString();

                if (mLastCalcTask != null) mLastCalcTask.cancel(true);
                mLastCalcTask = new CalcPasswordStrengthAsyncTask();
                mLastCalcTask.execute(password);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }


    class CalcPasswordStrengthAsyncTask extends AsyncTask<String, Void, PasswordStrengthResult> {

        @Override
        protected PasswordStrengthResult doInBackground(String... args) {

            // This will avoid updating the UI too often on quick password input
            for (int i=0; i<10; i++) {
                try { Thread.sleep(5); } catch(Exception e) {}
                if (isCancelled()) return null;
            }

            PasswordStrengthResult result = new PasswordStrengthResult();
            CharacterClass characterClass;
            String password = args[0];

            result.PasswordLength = password.length();
            if (result.PasswordLength <= 5) result.StrengthPoints = 0;
            else if (result.PasswordLength <= 8) result.StrengthPoints = 1;
            else if (result.PasswordLength <= 10) result.StrengthPoints = 2;
            else if (result.PasswordLength <= 13) result.StrengthPoints = 5;
            else if (result.PasswordLength <= 16) result.StrengthPoints = 10;
            else result.StrengthPoints = 30;

            for (int i=0; i < result.PasswordLength; i++) {

                char c = password.charAt(i);

                if (c >= 65 && c <= 90) characterClass = CharacterClass.Uppercase;
                else if (c >= 97 && c <= 122) characterClass = CharacterClass.Lowercase;
                else if (c >= 48 && c <= 57) characterClass = CharacterClass.Digit;
                else characterClass = CharacterClass.Symbol;

                Integer count = result.CharClassCounts.get(characterClass);
                if (count == null) count = 0;

                if (count == 0) result.StrengthPoints+=1; // increase only once per class
                result.CharClassCounts.put(characterClass, count + 1);

                if (isCancelled()) return null;
            }

            if (result.StrengthPoints < STRENGTH_POINTS_MIN_MEDIUM) result.Rating = PasswordRating.Poor;
            else if (result.StrengthPoints < STRENGTH_POINTS_MIN_GOOD) result.Rating = PasswordRating.Medium;
            else result.Rating = PasswordRating.Good;

            // Last chance to detect cancellation
            if (isCancelled()) return null;

            return result;
        }

        @Override
        protected void onPostExecute(PasswordStrengthResult result) {

            if (result == null) return;

            try {
                TextView txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);
                TextView txtPasswordWarning = mPassStrengthLayout.findViewById(R.id.txtPasswordWarning);
                ImageView imgUppercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsUppercase);
                ImageView imgLowercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsLowercase);
                ImageView imgDigits = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsDigits);
                ImageView imgSymbols = mPassStrengthLayout.findViewById(R.id.imgSymbols);
                ProgressBar progressPwStrength = mPassStrengthLayout.findViewById(R.id.progressPasswordStrength);

                progressPwStrength.setMax(STRENGTH_POINTS_MIN_GOOD);
                progressPwStrength.setProgress(result.StrengthPoints);

                switch (result.Rating) {

                    case Poor:
                        txtPasswordStrength.setText(mContext.getText(R.string.password_strength_poor));
                        txtPasswordStrength.setBackgroundColor(
                                ContextCompat.getColor(mContext, R.color.password_strength_poor));
                        break;

                    case Medium:
                        txtPasswordStrength.setText(mContext.getText(R.string.password_strength_medium));
                        txtPasswordStrength.setBackgroundColor(
                                ContextCompat.getColor(mContext, R.color.password_strength_medium));
                        break;

                    case Good:
                        txtPasswordStrength.setText(mContext.getText(R.string.password_strength_good));
                        txtPasswordStrength.setBackgroundColor(
                                ContextCompat.getColor(mContext, R.color.password_strength_good));
                        break;
                }

                ImageView iv = null;
                for (CharacterClass cc : result.CharClassCounts.keySet()) {
                    if (cc == CharacterClass.Lowercase) iv = imgLowercase;
                    if (cc == CharacterClass.Uppercase) iv = imgUppercase;
                    if (cc == CharacterClass.Digit) iv = imgDigits;
                    if (cc == CharacterClass.Symbol) iv = imgSymbols;

                    int count = result.CharClassCounts.get(cc);

                    Drawable drawable = count > 0 ?
                            ContextCompat.getDrawable(mContext, R.drawable.led_green) :
                            ContextCompat.getDrawable(mContext, R.drawable.led_red);

                    if (iv != null) iv.setImageDrawable(drawable);
                }

                if (result.PasswordLength > 0 && result.PasswordLength < PW_MIN_LENGTH) {
                    txtPasswordWarning.setText(R.string.short_password_warning);
                    txtPasswordWarning.setVisibility(View.VISIBLE);
                }
                else txtPasswordWarning.setVisibility(View.GONE);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class PasswordStrengthResult {

        public int StrengthPoints = 0;
        public int PasswordLength = 0;
        public Map<CharacterClass, Integer> CharClassCounts;
        public PasswordRating Rating = PasswordRating.Poor;

        public PasswordStrengthResult() {

            CharClassCounts = new HashMap<>();

            for (CharacterClass cc : CharacterClass.values()) {
                CharClassCounts.put(cc, 0);
            }
        }
    }

    protected enum CharacterClass {

        Lowercase,
        Uppercase,
        Digit,
        Symbol
    }

    protected enum PasswordRating {

        Poor,
        Medium,
        Good
    }
}
