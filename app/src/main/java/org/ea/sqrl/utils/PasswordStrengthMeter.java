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

public class PasswordStrengthMeter {

    private final Context mContext;
    private final boolean mUsePasswordList;
    private EditText mPasswordField;
    private ViewGroup mPassStrengthLayout;
    private final HashSet<String> mPasswordList = new HashSet<>(10000);


    public PasswordStrengthMeter(Context context, boolean usePasswordList) {

        mContext = context;
        mUsePasswordList = usePasswordList;

        if (usePasswordList) {
            new LoadPasswordListAsyncTask(mContext)
                    .execute();
        }
    }

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

    class StrengthResult {

        public Map<CharacterClass, Integer> CharClassCounts;
        public int StrengthPoints = 0;
        public boolean IsCommonPassword = false;

        public StrengthResult() {

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


    class CalcPasswordStrengthAsyncTask extends AsyncTask<String, Void, StrengthResult> {

        private final Context mContext;

        CalcPasswordStrengthAsyncTask(Context context) {

            mContext = context;
        }

        @Override
        protected StrengthResult doInBackground(String... args) {

            CharacterClass characterClass;
            StrengthResult result = new StrengthResult();
            String password = args[0];

            // Check common passwords list
            if (mUsePasswordList) {
                if (mPasswordList.contains(password)) {
                    result.IsCommonPassword = true;
                    return result;
                }
            }

            // Start counting
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

                if (count == 0) result.StrengthPoints++; // increase only for the first of each class
                result.CharClassCounts.put(characterClass, count + 1);
            }

            return result;

        }

        @Override
        protected void onPostExecute(StrengthResult result) {

            Map<ImageView, Integer> countMap = new HashMap<>();
            TextView txtPasswordStrength;
            TextView txtCommonPasswordWarning;

            try {
                txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);
                txtCommonPasswordWarning = mPassStrengthLayout.findViewById(R.id.txtCommonPasswordWarning);

                if (result.StrengthPoints <= 3)
                {
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_bad));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_bad));
                }
                else if (result.StrengthPoints <= 6) {
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_medium));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_medium));
                }
                else if (result.StrengthPoints <= 9){
                    txtPasswordStrength.setText(mContext.getText(R.string.password_strength_good));
                    txtPasswordStrength.setBackgroundColor(
                            ContextCompat.getColor(mContext, R.color.password_strength_good));
                }

                ImageView imgUppercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsUppercase);
                ImageView imgLowercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsLowercase);
                ImageView imgDigits = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsDigits);
                ImageView imgSymbols = mPassStrengthLayout.findViewById(R.id.imgSymbols);

                countMap.put(imgUppercase, result.CharClassCounts.get(CharacterClass.Uppercase));
                countMap.put(imgLowercase, result.CharClassCounts.get(CharacterClass.Lowercase));
                countMap.put(imgDigits, result.CharClassCounts.get(CharacterClass.Digit));
                countMap.put(imgSymbols, result.CharClassCounts.get(CharacterClass.Symbol));

                for (ImageView iv : countMap.keySet()) {

                    int count = countMap.get(iv);
                    Drawable drawable = count > 0 ?
                            ContextCompat.getDrawable(mContext, R.drawable.led_green) :
                            ContextCompat.getDrawable(mContext, R.drawable.led_red);
                    iv.setImageDrawable(drawable);
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
