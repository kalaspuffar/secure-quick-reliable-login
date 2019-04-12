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

import org.ea.sqrl.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Calculates and displays a password strength rating. Optionally, it also compares
 * the password against a list of common and thus unsafe passwords.
 *
 * It expects the password_strength_meter.xml to be present in the calling layout
 *
 * @author Alexander Hauser (alexhauser)
 */
public class PasswordStrengthMeter {

    private final int STRENGTH_POINTS_MIN_MEDIUM = 5;
    private final int STRENGTH_POINTS_MIN_GOOD = 9;
    private final Context mContext;
    private final boolean mCheckForCommonPasswords;
    private final HashSet<String> mCommonPasswordList = new HashSet<>(10000);
    private ViewGroup mPassStrengthLayout;
    private CalcPasswordStrengthAsyncTask mLastCalcTask;


    /**
     * Creates a PasswordStrengthMeter object and optionally loads the common password list to memory.
     *
     * @param context The context of the caller.
     * @param checkForCommonPasswords Should be true if a check against a list of common passwords is desired, or false otherwise.
     */
    public PasswordStrengthMeter(Context context, boolean checkForCommonPasswords) {

        mContext = context;
        mCheckForCommonPasswords = checkForCommonPasswords;

        if (checkForCommonPasswords) {
            new LoadPasswordListAsyncTask()
                    .execute();
        }
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

    class PasswordStrengthResult {

        public Map<CharacterClass, Integer> CharClassCounts;
        public int StrengthPoints = 0;
        public boolean IsCommonPassword = false;
        public PasswordRating Rating = PasswordRating.Poor;

        public PasswordStrengthResult() {

            CharClassCounts = new HashMap<>();

            for (CharacterClass cc : CharacterClass.values()) {
                CharClassCounts.put(cc, 0);
            }
        }
    }


    class LoadPasswordListAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            BufferedReader reader;
            InputStream fileInputStream;

            try {
                mCommonPasswordList.clear();

                fileInputStream = mContext.getResources().openRawResource(
                        mContext.getResources().getIdentifier("most_common_passwords_10k",
                                "raw", mContext.getPackageName()));
                reader = new BufferedReader(new InputStreamReader(fileInputStream));
                String line = reader.readLine();
                while (line != null) {
                    mCommonPasswordList.add(line);
                    if (isCancelled()) return null;
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            /*
            Toast.makeText(mContext, "Password list imported! Elements: " + mCommonPasswordList.size(),
                Toast.LENGTH_SHORT).show();
            */
        }
    }


    class CalcPasswordStrengthAsyncTask extends AsyncTask<String, Void, PasswordStrengthResult> {

        @Override
        protected PasswordStrengthResult doInBackground(String... args) {

            // This will avoid updating the UI too often on quick password input
            for (int i=0; i<10; i++) {
                try { Thread.sleep(5); } catch(Exception e) {}
                if (isCancelled()) return null;
            }

            CharacterClass characterClass;
            PasswordStrengthResult result = new PasswordStrengthResult();
            String password = args[0];
            int passwordLength = password.length();

            if (passwordLength <= 5) result.StrengthPoints = 0;
            else if (passwordLength <= 8) result.StrengthPoints = 1;
            else if (passwordLength <= 10) result.StrengthPoints = 2;
            else if (passwordLength <= 13) result.StrengthPoints = 5;
            else if (passwordLength <= 16) result.StrengthPoints = 10;
            else result.StrengthPoints = 30;

            for (int i=0; i<passwordLength; i++) {

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

            // Check common passwords list
            if (mCheckForCommonPasswords) {
                if (mCommonPasswordList.contains(password)) {
                    result.IsCommonPassword = true;
                    result.StrengthPoints = 0;
                }
            }

            if (isCancelled()) return null;

            if (result.StrengthPoints < STRENGTH_POINTS_MIN_MEDIUM) result.Rating = PasswordRating.Poor;
            else if (result.StrengthPoints < STRENGTH_POINTS_MIN_GOOD) result.Rating = PasswordRating.Medium;
            else result.Rating = PasswordRating.Good;

            return result;

        }

        @Override
        protected void onPostExecute(PasswordStrengthResult result) {

            if (result == null) return;

            try {
                TextView txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);
                TextView txtCommonPasswordWarning = mPassStrengthLayout.findViewById(R.id.txtCommonPasswordWarning);
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

                if (mCheckForCommonPasswords && result.IsCommonPassword)
                    txtCommonPasswordWarning.setVisibility(View.VISIBLE);
                else txtCommonPasswordWarning.setVisibility(View.GONE);

                /*
                Toast.makeText(mContext, "Score: " + result.StrengthPoints,
                        Toast.LENGTH_SHORT).show();
                */
            }
            catch (Exception e) {
                e.printStackTrace();
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
