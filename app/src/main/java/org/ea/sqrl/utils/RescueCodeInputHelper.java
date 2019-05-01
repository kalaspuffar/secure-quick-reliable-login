package org.ea.sqrl.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.List;

/**
 * Helper class for handling rescue code input and validation.
 * Expects the rescue_code_input.xml to be present in the calling layout
 *
 * @author Alexander Hauser (alexhauser)
 */
public class RescueCodeInputHelper {

    public interface StatusChangedListener {
        void onEvent(boolean successfullyCompleted);
    }

    private static final String TAG = "RescueCodeInputHelper";
    private Context mContext;
    private ViewGroup mLayoutRoot;
    private EditText mTxtRecoverCode1;
    private EditText mTxtRecoverCode2;
    private EditText mTxtRecoverCode3;
    private EditText mTxtRecoverCode4;
    private EditText mTxtRecoverCode5;
    private EditText mTxtRecoverCode6;
    private SQRLStorage mSqrlStorage;
    private List<String> mRescueList;
    private boolean mValidateInput = false;
    private boolean mLastStatus = false;
    private StatusChangedListener mStatusChangedListener;


    /**
     * Creates a new RescueCodeInputHelper object and registers event listeners
     * on the rescue code input form.
     *
     * @param context       The context of the caller.
     * @param rootLayout    The root layout containing the rescue_code_input layout
     *                      as well as the nextFocusDown view.
     * @param nextFocusDown The View that should receive focus after the last
     *                      rescue code field has been filled out.
     * @param validateInput Must be true only if the rescue code was just created and is now being
     *                      verified. In this mode, input is being compared to the actual rescue code
     *                      and errors are being displayed while typing. Should be set to false in
     *                      every other case.
     */
    public RescueCodeInputHelper (Context context, ViewGroup rootLayout,
                                  @Nullable View nextFocusDown, boolean validateInput) {

        mContext = context;
        mLayoutRoot = rootLayout;
        mValidateInput = validateInput;

        mTxtRecoverCode1 = mLayoutRoot.findViewById(R.id.txtRecoverCode1);
        mTxtRecoverCode2 = mLayoutRoot.findViewById(R.id.txtRecoverCode2);
        mTxtRecoverCode3 = mLayoutRoot.findViewById(R.id.txtRecoverCode3);
        mTxtRecoverCode4 = mLayoutRoot.findViewById(R.id.txtRecoverCode4);
        mTxtRecoverCode5 = mLayoutRoot.findViewById(R.id.txtRecoverCode5);
        mTxtRecoverCode6 = mLayoutRoot.findViewById(R.id.txtRecoverCode6);

        if (nextFocusDown != null) {
            mTxtRecoverCode6.setNextFocusDownId(nextFocusDown.getId());
        }

        if (mValidateInput) {
            mSqrlStorage = SQRLStorage.getInstance(mContext);
            mRescueList = mSqrlStorage.getTempShowableRescueCode();
        }

        setListener(mTxtRecoverCode1);
        setListener(mTxtRecoverCode2);
        setListener(mTxtRecoverCode3);
        setListener(mTxtRecoverCode4);
        setListener(mTxtRecoverCode5);
        setListener(mTxtRecoverCode6);
    }

    /**
     * Registers a callback to be invoked when the status of the rescue code input changes.
     *
     * @param listener The callback method that should be executed.
     */
    public void setStatusChangedListener(StatusChangedListener listener) {

        mStatusChangedListener = listener;
    }

    /**
     * Checks if the rescue code input is complete and correct.
     * If "verify" was set to true in the constructor, this checks
     *
     * @return True if the rescue code input is complete and correct, false otherwise.
     */
    public boolean getStatus() {

        return checkEditText(mTxtRecoverCode1, mValidateInput ? mRescueList.get(0) : null) &&
                checkEditText(mTxtRecoverCode2, mValidateInput ? mRescueList.get(1) : null) &&
                checkEditText(mTxtRecoverCode3, mValidateInput ? mRescueList.get(2) : null) &&
                checkEditText(mTxtRecoverCode4, mValidateInput ? mRescueList.get(3) : null) &&
                checkEditText(mTxtRecoverCode5, mValidateInput ? mRescueList.get(4) : null) &&
                checkEditText(mTxtRecoverCode6, mValidateInput ? mRescueList.get(5) : null);
    }

    /**
     * Requests focus for the first rescue code input field.
     */
    public void requestFocus() {

        mTxtRecoverCode1.requestFocus();
    }

    /**
     * Retrieves the entered rescue code as a String.
     *
     * @return The entered rescue code as a String.
     */
    public String getRescueCodeInput() {

        StringBuilder rescueCode = new StringBuilder();
        rescueCode.append(mTxtRecoverCode1.getText().toString());
        rescueCode.append(mTxtRecoverCode2.getText().toString());
        rescueCode.append(mTxtRecoverCode3.getText().toString());
        rescueCode.append(mTxtRecoverCode4.getText().toString());
        rescueCode.append(mTxtRecoverCode5.getText().toString());
        rescueCode.append(mTxtRecoverCode6.getText().toString());
        return rescueCode.toString();
    }

    /**
     * Clears the rescue code input form
     */
    public void clearForm() {

        mTxtRecoverCode1.setText("");
        mTxtRecoverCode2.setText("");
        mTxtRecoverCode3.setText("");
        mTxtRecoverCode4.setText("");
        mTxtRecoverCode5.setText("");
        mTxtRecoverCode6.setText("");
    }

    /**
     * Fills the given rescue code into the input form.
     *
     * @param rescueArr A list containing the six parts of the rescue code.
     */
    public void setRescueCodeInput(List<String> rescueArr) {

        mTxtRecoverCode1.setText(rescueArr.get(0));
        mTxtRecoverCode2.setText(rescueArr.get(1));
        mTxtRecoverCode3.setText(rescueArr.get(2));
        mTxtRecoverCode4.setText(rescueArr.get(3));
        mTxtRecoverCode5.setText(rescueArr.get(4));
        mTxtRecoverCode6.setText(rescueArr.get(5));
    }

    /**
     * Enable or disable form input.
     *
     * @param enabled Set to true if form input should be enabled, otherwise set to false.
     */
    public void setInputEnabled(boolean enabled) {

        mTxtRecoverCode1.setEnabled(enabled);
        mTxtRecoverCode2.setEnabled(enabled);
        mTxtRecoverCode3.setEnabled(enabled);
        mTxtRecoverCode4.setEnabled(enabled);
        mTxtRecoverCode5.setEnabled(enabled);
        mTxtRecoverCode6.setEnabled(enabled);
    }

    private void setListener(EditText code) {

        code.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                boolean status = getStatus();

                if (status != mLastStatus) {
                    if(mStatusChangedListener != null) {
                        mStatusChangedListener.onEvent(status);
                    }
                    mLastStatus = status;
                }
            }
        });
    }

    private boolean checkEditText(EditText code, String verify) {

        String check = code.getText().toString();

        if(check.length() != 4) return false;

        boolean correct = true;

        if (verify != null) {
            if (!check.equals(verify)) {
                correct = false;
            }
        }

        try {
            Integer.parseInt(check);
        } catch (NumberFormatException nfe) {
            correct = false;
        }

        if (!correct) {
            code.setError(mContext.getResources().getString(
                    R.string.rescue_code_incorrect));
            return false;
        }

        code.setError(null);
        View nextFocusDown = mLayoutRoot.findViewById(code.getNextFocusDownId());
        if (nextFocusDown != null) nextFocusDown.requestFocus();
        return true;
    }
}
