package org.ea.sqrl.activites.create;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.List;

/**
 *
 * @author Daniel Persson
 */
public class RescueCodeEnterActivity extends AppCompatActivity {
    private static final String TAG = "RescueCodeEnterActivity";
    private EditText txtRecoverCode1;
    private EditText txtRecoverCode2;
    private EditText txtRecoverCode3;
    private EditText txtRecoverCode4;
    private EditText txtRecoverCode5;
    private EditText txtRecoverCode6;
    private Button btnRescueCodeEnterNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuecode_enter);

        txtRecoverCode1 = findViewById(R.id.txtRecoverCode1);
        txtRecoverCode2 = findViewById(R.id.txtRecoverCode2);
        txtRecoverCode3 = findViewById(R.id.txtRecoverCode3);
        txtRecoverCode4 = findViewById(R.id.txtRecoverCode4);
        txtRecoverCode5 = findViewById(R.id.txtRecoverCode5);
        txtRecoverCode6 = findViewById(R.id.txtRecoverCode6);

        btnRescueCodeEnterNext = findViewById(R.id.btnRescueCodeEnterNext);
        btnRescueCodeEnterNext.setEnabled(false);
        btnRescueCodeEnterNext.setOnClickListener(v -> {
            this.finish();
            startActivity(new Intent(this, SaveIdentityActivity.class));
        });

        boolean runningTest = getIntent().getBooleanExtra("RUNNING_TEST", false);
        if(runningTest) return;

        SQRLStorage storage = SQRLStorage.getInstance();
        List<String> rescueList = storage.getTempShowableRescueCode();

        setListener(txtRecoverCode1, rescueList);
        setListener(txtRecoverCode2, rescueList);
        setListener(txtRecoverCode3, rescueList);
        setListener(txtRecoverCode4, rescueList);
        setListener(txtRecoverCode5, rescueList);
        setListener(txtRecoverCode6, rescueList);

    }

    private void setListener(EditText code, List<String> rescueList) {
        code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 4) {
                    boolean incorrect =
                            checkEditText(txtRecoverCode1, rescueList.get(0)) ||
                            checkEditText(txtRecoverCode2, rescueList.get(1)) ||
                            checkEditText(txtRecoverCode3, rescueList.get(2)) ||
                            checkEditText(txtRecoverCode4, rescueList.get(3)) ||
                            checkEditText(txtRecoverCode5, rescueList.get(4)) ||
                            checkEditText(txtRecoverCode6, rescueList.get(5));
                    btnRescueCodeEnterNext.setEnabled(!incorrect);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean checkEditText(EditText code, String verify) {
        if (code.getText().toString().equals(verify)) {
            code.setError(null);
            return false;
        } else {
            if(code.getText().length() > 0) {
                code.setError("Incorrect");
            }
            return true;
        }
    }
}
