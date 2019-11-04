package org.ea.sqrl.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.ea.sqrl.R;

/**
 * Calculates and displays a password strength rating.
 * Expects the password_strength_meter.xml to be present in the calling layout
 *
 * @author Alexander Hauser (alexhauser)
 */
public class PasswordStrengthMeter {

    private final int PW_MIN_LENGTH = 8;
    private final int STRENGTH_POINTS_MIN_MEDIUM = 11;
    private final int STRENGTH_POINTS_MIN_GOOD = 17;
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
            String password = args[0];

            result.passwordLength = password.length();
            if (result.passwordLength > PW_MIN_LENGTH) {
                result.strengthPoints = result.passwordLength;
            } else {
                result.strengthPoints = (int)(result.passwordLength/2);
            }

            for (int i = 0; i < password.length(); i++) {

                char c = password.charAt(i);

                if (c >= 65 && c <= 90) {       // Uppercase
                    if (!result.uppercaseUsed) {
                        result.strengthPoints += 2;
                        result.uppercaseUsed = true;
                    }
                } else if (c >= 97 && c <= 122) { // Lowercase
                    if (!result.lowercaseUsed) {
                        result.lowercaseUsed = true;
                    }
                } else if (c >= 48 && c <= 57) {  // Digit
                    if (!result.digitsUsed) {
                        result.strengthPoints += 2;
                        result.digitsUsed = true;
                    }
                } else {                          // Symbol
                    if (!result.symbolsUsed) {
                        result.strengthPoints += 2;
                        result.symbolsUsed = true;
                    }
                }

                if (result.allCharClassesUsed()) break; // No need to look any further
                if (isCancelled()) return null;
            }

            if (result.strengthPoints < STRENGTH_POINTS_MIN_MEDIUM) {
                result.rating = PasswordRating.POOR;
            } else if (result.strengthPoints < STRENGTH_POINTS_MIN_GOOD) {
                result.rating = PasswordRating.MEDIUM;
            } else result.rating = PasswordRating.GOOD;

            // Last chance to detect cancellation
            if (isCancelled()) return null;

            return result;
        }

        @Override
        protected void onPostExecute(PasswordStrengthResult result) {

            if (result == null) return;

            try {
                Drawable ledGreen = ContextCompat.getDrawable(mContext, R.drawable.led_green);
                Drawable ledRed   = ContextCompat.getDrawable(mContext, R.drawable.led_red);
                TextView txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);
                TextView txtPasswordWarning = mPassStrengthLayout.findViewById(R.id.txtPasswordWarning);
                ImageView imgUppercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsUppercase);
                ImageView imgLowercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsLowercase);
                ImageView imgDigits = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsDigits);
                ImageView imgSymbols = mPassStrengthLayout.findViewById(R.id.imgSymbols);
                ProgressBar progressPwStrength = mPassStrengthLayout.findViewById(R.id.progressPasswordStrength);

                progressPwStrength.setMax(STRENGTH_POINTS_MIN_GOOD);
                progressPwStrength.setProgress(result.strengthPoints);

                if (result.rating == PasswordRating.POOR) {
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_poor));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_poor));
                } else if (result.rating == PasswordRating.MEDIUM) {
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_medium));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_medium));
                } else if (result.rating == PasswordRating.GOOD) {
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_good));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_good));
                }

                imgLowercase.setImageDrawable(result.lowercaseUsed ? ledGreen : ledRed);
                imgUppercase.setImageDrawable(result.uppercaseUsed ? ledGreen : ledRed);
                imgDigits.setImageDrawable(result.digitsUsed ? ledGreen : ledRed);
                imgSymbols.setImageDrawable(result.symbolsUsed ? ledGreen : ledRed);

                if (result.passwordLength > 0 && result.passwordLength < PW_MIN_LENGTH) {
                    txtPasswordWarning.setText(R.string.short_password_warning);
                    txtPasswordWarning.setVisibility(View.VISIBLE);
                } else {
                    txtPasswordWarning.setVisibility(View.GONE);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class PasswordStrengthResult {

        public int strengthPoints = 0;
        public int passwordLength = 0;

        public boolean lowercaseUsed = false;
        public boolean uppercaseUsed = false;
        public boolean digitsUsed = false;
        public boolean symbolsUsed = false;

        public PasswordRating rating = PasswordRating.POOR;

        public boolean allCharClassesUsed() {
            return (lowercaseUsed && uppercaseUsed && digitsUsed && symbolsUsed);
        }
    }

    protected enum PasswordRating {

        POOR,
        MEDIUM,
        GOOD
    }
}
