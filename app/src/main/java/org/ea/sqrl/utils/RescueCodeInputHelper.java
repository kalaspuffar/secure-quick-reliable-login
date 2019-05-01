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
    private boolean mDisplayErrors = false;
    private boolean mLastStatus = false;
    private StatusChangedListener mStatusChangedListener;


    /**
     * Creates a RescueCodeInputHelper object.
     *
     * @param context The context of the caller.
     */
    public RescueCodeInputHelper (Context context) {

        mContext = context;
        mSqrlStorage = SQRLStorage.getInstance(mContext);
        mRescueList = mSqrlStorage.getTempShowableRescueCode();
    }

    /**
     * Registers the RescueCodeInputHelper to set event handlers on the rescue code input form
     * and enables the corresponding events.
     *
     * @param rootLayout The root layout containing the rescue_code_input layout as well as the nextFocusDown view.
     * @param nextFocusDown The View that should receive focus after the last rescue code field has been filled out.
     */
    public void register(ViewGroup rootLayout, @Nullable View nextFocusDown) {

        mLayoutRoot = rootLayout;

        mTxtRecoverCode1 = mLayoutRoot.findViewById(R.id.txtRecoverCode1);
        mTxtRecoverCode2 = mLayoutRoot.findViewById(R.id.txtRecoverCode2);
        mTxtRecoverCode3 = mLayoutRoot.findViewById(R.id.txtRecoverCode3);
        mTxtRecoverCode4 = mLayoutRoot.findViewById(R.id.txtRecoverCode4);
        mTxtRecoverCode5 = mLayoutRoot.findViewById(R.id.txtRecoverCode5);
        mTxtRecoverCode6 = mLayoutRoot.findViewById(R.id.txtRecoverCode6);

        if (nextFocusDown != null) {
            mTxtRecoverCode6.setNextFocusDownId(nextFocusDown.getId());
        }

        setListener(mTxtRecoverCode1);
        setListener(mTxtRecoverCode2);
        setListener(mTxtRecoverCode3);
        setListener(mTxtRecoverCode4);
        setListener(mTxtRecoverCode5);
        setListener(mTxtRecoverCode6);
    }

    /**
     * Enables or disables showing input errors in the input form's UI.
     *
     * @param displayErrors Set to true if input errors should be displayed, otherwise to false.
     */
    public void setDisplayErrors(boolean displayErrors) {

        mDisplayErrors = displayErrors;
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
     *
     * @return True if the rescue code input is complete and correct, false otherwise.
     */
    public boolean getStatus() {

        return checkEditText(mTxtRecoverCode1, mRescueList.get(0)) &&
            checkEditText(mTxtRecoverCode2, mRescueList.get(1)) &&
            checkEditText(mTxtRecoverCode3, mRescueList.get(2)) &&
            checkEditText(mTxtRecoverCode4, mRescueList.get(3)) &&
            checkEditText(mTxtRecoverCode5, mRescueList.get(4)) &&
            checkEditText(mTxtRecoverCode6, mRescueList.get(5));
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

        if (check.equals(verify)) {
            code.setError(null);
            View nextFocusDown = mLayoutRoot.findViewById(code.getNextFocusDownId());
            if (nextFocusDown != null) nextFocusDown.requestFocus();
            return true;
        } else {
            if(mDisplayErrors) {
                code.setError(mContext.getResources().getString(
                        R.string.rescue_code_incorrect));
            }
            return false;
        }
    }
}
