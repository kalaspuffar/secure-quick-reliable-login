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

/**
 * Calculates and displays a password strength rating. Optionally, it also compares
 * the password against a list of common and thus unsafe passwords.
 * All expensive operations are executed on a background thread.
 *
 * It expects the password_strength_meter.xml to be present in the calling layout
 *
 * @author Alexander Hauser (alexhauser)
 */
public class PasswordStrengthMeter {

    private final Context mContext;
    private final boolean mUsePasswordList;
    private final HashSet<String> mPasswordList = new HashSet<>(10000);
    private EditText mPasswordField;
    private ViewGroup mPassStrengthLayout;


    /**
     * Creates a PasswordStrengthMeter object and optionally loads the common password list to memory.
     *
     * @param context The context of the caller.
     * @param usePasswordList Should be true if a check against a list of common passwords is desired, or false otherwise.
     */
    public PasswordStrengthMeter(Context context, boolean usePasswordList) {

        mContext = context;
        mUsePasswordList = usePasswordList;

        if (usePasswordList) {
            new LoadPasswordListAsyncTask(mContext)
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

        mPasswordField = passwordField;
        mPassStrengthLayout = passStrengthLayout;
        mPasswordField.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String password = s.toString();
                new CalcPasswordStrengthAsyncTask(mContext).execute(password);
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


    class LoadPasswordListAsyncTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        LoadPasswordListAsyncTask(Context context) {

            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            BufferedReader reader;
            InputStream fileInputStream;

            try {
                mPasswordList.clear();

                fileInputStream = mContext.getResources().openRawResource(
                        mContext.getResources().getIdentifier("most_common_passwords_10k",
                                "raw", mContext.getPackageName()));
                reader = new BufferedReader(new InputStreamReader(fileInputStream));
                String line = reader.readLine();
                while (line != null) {
                    mPasswordList.add(line);
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

            Toast.makeText(mContext, "Password list imported! Elements: " + mPasswordList.size(),
                    Toast.LENGTH_LONG).show();
        }
    }


    class CalcPasswordStrengthAsyncTask extends AsyncTask<String, Void, PasswordStrengthResult> {

        private final Context mContext;

        CalcPasswordStrengthAsyncTask(Context context) {

            mContext = context;
        }

        @Override
        protected PasswordStrengthResult doInBackground(String... args) {

            CharacterClass characterClass;
            PasswordStrengthResult result = new PasswordStrengthResult();
            String password = args[0];
            int passwordLength = password.length();

            if (passwordLength <= 7) result.StrengthPoints = 0;
            else if (passwordLength <= 10) result.StrengthPoints = 2;
            else if (passwordLength <= 15) result.StrengthPoints = 3;
            else result.StrengthPoints = 5;

            for (int i=0; i<passwordLength; i++) {

                char c = password.charAt(i);

                if (c >= 65 && c <= 90) characterClass = CharacterClass.Uppercase;
                else if (c >= 97 && c <= 122) characterClass = CharacterClass.Lowercase;
                else if (c >= 48 && c <= 57) characterClass = CharacterClass.Digit;
                else characterClass = CharacterClass.Symbol;

                Integer count = result.CharClassCounts.get(characterClass);
                if (count == null) count = 0;

                if (count == 0) result.StrengthPoints++; // increase only once per class
                result.CharClassCounts.put(characterClass, count + 1);
            }

            // Check common passwords list
            if (mUsePasswordList) {
                if (mPasswordList.contains(password)) {
                    result.IsCommonPassword = true;
                    result.StrengthPoints = 0;
                }
            }

            if (result.StrengthPoints <= 3) result.Rating = PasswordRating.Poor;
            else if (result.StrengthPoints <= 6) result.Rating = PasswordRating.Medium;
            else if (result.StrengthPoints <= 9) result.Rating = PasswordRating.Good;

            return result;

        }

        @Override
        protected void onPostExecute(PasswordStrengthResult result) {

            try {
                TextView txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);
                TextView txtCommonPasswordWarning = mPassStrengthLayout.findViewById(R.id.txtCommonPasswordWarning);
                ImageView imgUppercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsUppercase);
                ImageView imgLowercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsLowercase);
                ImageView imgDigits = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsDigits);
                ImageView imgSymbols = mPassStrengthLayout.findViewById(R.id.imgSymbols);

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

                if (result.IsCommonPassword)
                    txtCommonPasswordWarning.setVisibility(View.VISIBLE);
                else txtCommonPasswordWarning.setVisibility(View.GONE);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
