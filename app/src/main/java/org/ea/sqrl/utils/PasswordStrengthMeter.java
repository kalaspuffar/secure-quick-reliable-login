package org.ea.sqrl.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;

import java.util.HashMap;
import java.util.Map;

public class PasswordStrengthMeter {

    private final Context mContext;
    private final EditText mPasswordField;
    private final ViewGroup mPassStrengthLayout;
    private final Map<CharacterClass, Integer> mCharClassCounts = new HashMap<>();
    private int mStrengthPoints = 0;


    public PasswordStrengthMeter(Context context, EditText passwordField, ViewGroup passStrengthLayout) {

        mContext = context;
        mPasswordField = passwordField;
        mPassStrengthLayout = passStrengthLayout;

        SetHandlers();
    }

    private void SetHandlers() {

        mPasswordField.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String password = s.toString();
                updateStats(password);
                updateUi();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void updateStats(String password) {

        CharacterClass characterClass;

        // Reset counters
        mStrengthPoints = 0;
        mCharClassCounts.clear();
        for (CharacterClass cc : CharacterClass.values()) {
            mCharClassCounts.put(cc, 0);
        }

        // Start counting
        int passwordLength = password.length();
        if (passwordLength <= 5) mStrengthPoints = 1;
        else if (passwordLength <= 10) mStrengthPoints = 2;
        else if (passwordLength <= 15) mStrengthPoints = 3;
        else mStrengthPoints = 4;

        for (int i=0; i<passwordLength; i++) {

            char c = password.charAt(i);

            if (c >= 65 && c <= 90) characterClass = CharacterClass.Uppercase;
            else if (c >= 97 && c <= 122) characterClass = CharacterClass.Lowercase;
            else if (c >= 48 && c <= 57) characterClass = CharacterClass.Digit;
            else characterClass = CharacterClass.Symbol;

            Integer count = mCharClassCounts.get(characterClass);
            if (count == null) count = 0;

            if (count == 0) mStrengthPoints++; // increase only for the first of each class
            mCharClassCounts.put(characterClass, count + 1);
        }
    }

    private void updateUi() {

        Map<ImageView, Integer> countMap = new HashMap<>();

        try {
            // Update textual representation

            TextView txtPasswordStrength = mPassStrengthLayout.findViewById(R.id.txtPasswordStrength);

            if (mStrengthPoints <= 3)
            {
                txtPasswordStrength.setText(mContext.getText(R.string.password_strength_bad));
                txtPasswordStrength.setBackgroundColor(
                        ContextCompat.getColor(mContext, R.color.password_strength_bad));
            }
            else if (mStrengthPoints <= 6) {
                txtPasswordStrength.setText(mContext.getText(R.string.password_strength_medium));
                txtPasswordStrength.setBackgroundColor(
                        ContextCompat.getColor(mContext, R.color.password_strength_medium));
            }
            else if (mStrengthPoints <= 9){
                txtPasswordStrength.setText(mContext.getText(R.string.password_strength_good));
                txtPasswordStrength.setBackgroundColor(
                        ContextCompat.getColor(mContext, R.color.password_strength_good));
            }

            // Update graphical character class indicators

            ImageView imgUppercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsUppercase);
            ImageView imgLowercase = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsLowercase);
            ImageView imgDigits = mPassStrengthLayout.findViewById(R.id.imgPasswordContainsDigits);
            ImageView imgSymbols = mPassStrengthLayout.findViewById(R.id.imgSymbols);

            countMap.put(imgUppercase, mCharClassCounts.get(CharacterClass.Uppercase));
            countMap.put(imgLowercase, mCharClassCounts.get(CharacterClass.Lowercase));
            countMap.put(imgDigits, mCharClassCounts.get(CharacterClass.Digit));
            countMap.put(imgSymbols, mCharClassCounts.get(CharacterClass.Symbol));

            for (ImageView iv : countMap.keySet()) {

                int count = countMap.get(iv);
                Drawable drawable = count > 0 ?
                        ContextCompat.getDrawable(mContext, R.drawable.led_green) :
                        ContextCompat.getDrawable(mContext, R.drawable.led_red);
                iv.setImageDrawable(drawable);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected enum CharacterClass {

        Lowercase,
        Uppercase,
        Digit,
        Symbol
    }
}
