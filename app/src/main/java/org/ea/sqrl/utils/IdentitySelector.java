
package org.ea.sqrl.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SettingsActivity;
import org.ea.sqrl.activites.StartActivity;
import org.ea.sqrl.activites.create.RekeyIdentityActivity;
import org.ea.sqrl.activites.identity.ChangePasswordActivity;
import org.ea.sqrl.activites.identity.ExportOptionsActivity;
import org.ea.sqrl.activites.identity.RenameActivity;
import org.ea.sqrl.activites.identity.ResetPasswordActivity;
import org.ea.sqrl.database.IdentityDBHelper;

import java.util.Map;

/**
 * IdentitySelector aims to be the single source for switching identities within the app.
 * It expects the fragment_identity_selector.xml to be present in the calling layout.
 *
 * @author Alexander Hauser (alexhauser)
 */
public class IdentitySelector {

    public interface IdentityChangedListener {
        void onIdentityChanged(Long identityIndex, String identityName);
    }

    private static final String TAG = "IdentitySelector";
    private static final int ID_NAME_MAX_LEN = 30;

    private final Context mContext;
    private ViewGroup mIdentitySelectorLayout;
    private boolean mEnableIdentityChange;
    private boolean mHideOnSingleIdentity;
    private boolean mEnableIdentityOptions;
    private boolean mShowLayoutBorder;
    private boolean mTwoLinesCentered;
    private IdentityChangedListener mIdentityChangedListener = null;
    private Map<Long, String> mIdentities;
    private IdentityDBHelper mDbHelper = null;
    private ImageView mImgListIdentities = null;
    private ImageView mImgIdentityOptions = null;
    private TextView mTxtSelectedIdentityHeadline = null;
    private TextView mTxtSelectedIdentity = null;
    private long mLastId;

    /**
     * Creates an IdentitySelector object.
     *
     * @param context                The context of the caller.
     * @param enableIdentityChange   Set to true if selecting another identity should be enabled, or to false otherwise.
     * @param enableIdentityOptions  Set to true if identity options should be enabled, or to false otherwise.
     *                               If enabled, an options icon will be displayed in the identity selector layout.
     * @param showLayoutBorder       Set to true if a border should be drawn around the identity selector layout, false otherwise.
     * @param twoLinesCentered       Set to true if the layout should use two lines and align its contents centered, false otherwise.
     * @param hideOnSingleIdentity   Set to true if the identity selector layout should be hidden if only one identity exists.
     */
    public IdentitySelector(Context context, boolean enableIdentityChange, boolean enableIdentityOptions,
                            boolean showLayoutBorder, boolean twoLinesCentered, boolean hideOnSingleIdentity) {
        mContext = context;
        mEnableIdentityChange = enableIdentityChange;
        mEnableIdentityOptions = enableIdentityOptions;
        mShowLayoutBorder = showLayoutBorder;
        mTwoLinesCentered = twoLinesCentered;
        mHideOnSingleIdentity = hideOnSingleIdentity;
        mDbHelper = IdentityDBHelper.getInstance(mContext);
        mIdentities = mDbHelper.getIdentities();
        mLastId = -1;
    }

    /**
     * Registers the instance of the identity selector with the corresponding inflated layout
     *
     * @param identitySelectorLayout A ViewGroup containing the fragment_identity_selector.xml layout.
     */
    public void registerLayout(ViewGroup identitySelectorLayout) {

        if (identitySelectorLayout == null) return;
        mIdentitySelectorLayout = identitySelectorLayout;

        mTxtSelectedIdentityHeadline = mIdentitySelectorLayout.findViewById(R.id.txtSelectedIdentityHeadline);
        mTxtSelectedIdentity = mIdentitySelectorLayout.findViewById(R.id.txtSelectedIdentity);
        mImgListIdentities =  mIdentitySelectorLayout.findViewById(R.id.imgListIdentities);
        mImgIdentityOptions =  mIdentitySelectorLayout.findViewById(R.id.imgIdentityOptions);

        mImgListIdentities.setOnClickListener(mOnListIdentitiesClickListener);
        mImgIdentityOptions.setOnClickListener(mOnIdentitySettingsClickListener);

        mTxtSelectedIdentityHeadline.setText(
                mTxtSelectedIdentityHeadline.getText() + ":"
        );

        if (!mShowLayoutBorder) {
            mIdentitySelectorLayout.setBackground(null);
        }

        if (mTwoLinesCentered) {
            setTwoLinesCentered();
        }

        update();
    }

    /**
     * Re-reads the identity database and updates the identity selector accordingly.
     * This is useful when an "external" change has occurred, for example an addition,
     * removal or renaming of an identity.
     * It is generally a good idea to call this method in the onResume() event of an activity
     * where an IdentitySelector is deployed.
     */
    public void update() {
        long currentId = SqrlApplication.getCurrentId(mContext);
        String currentName = mDbHelper.getIdentityName(currentId);
        if (currentName == null) currentName = "";

        if (currentName.length() > ID_NAME_MAX_LEN) {
            currentName = currentName.substring(0, ID_NAME_MAX_LEN) + "...";
        }
        SpannableString ssCurrentName = new SpannableString(currentName);
        ssCurrentName.setSpan(new UnderlineSpan(), 0, ssCurrentName.length(), 0);
        mTxtSelectedIdentity.setText(ssCurrentName);
        mLastId = currentId;
        mIdentities = mDbHelper.getIdentities();

        if (mHideOnSingleIdentity && mIdentities.size() < 2) {
            mIdentitySelectorLayout.setVisibility(View.GONE);
            return;
        } else {
            mIdentitySelectorLayout.setVisibility(View.VISIBLE);
        }

        if (!mEnableIdentityChange || mIdentities.size() < 2) {
            mImgListIdentities.setVisibility(View.GONE);
            mTxtSelectedIdentity.setOnClickListener(null);
        } else {
            mImgListIdentities.setVisibility(View.VISIBLE);
            mTxtSelectedIdentity.setOnClickListener(mOnListIdentitiesClickListener);
        }

        mImgIdentityOptions.setVisibility( mEnableIdentityOptions ? View.VISIBLE : View.GONE);
    }

    /**
     * Sets a listener that will be called when the active identity changes.
     * @param listener The listener to call  when the active identity changes.
     */
    public void setIdentityChangedListener(IdentityChangedListener listener) {
        mIdentityChangedListener = listener;
    }

    private void selectIdentityInternal(int index) {
        Long[] keyArray = mIdentities.keySet().toArray(new Long[mIdentities.size()]);
        long currentId = keyArray[index];
        String currentName = mDbHelper.getIdentityName(currentId);

        if (mLastId == currentId) return;

        SqrlApplication.setCurrentId(mContext, currentId);

        update();

        if (mIdentityChangedListener != null) {
            mIdentityChangedListener.onIdentityChanged(currentId, currentName);
        }
    }

    private int getIndexFromId(long currentId) {
        if (mIdentities == null) return -1;

        int i = 0;
        for(long l : mIdentities.keySet()) {
            if (l == currentId) return i;
            i++;
        }
        return -1;
    }

    private void setTwoLinesCentered() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout)mIdentitySelectorLayout);

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        mTxtSelectedIdentityHeadline.setLayoutParams(lp);

        constraintSet.connect(mTxtSelectedIdentityHeadline.getId(),ConstraintSet.LEFT,
                mIdentitySelectorLayout.getId(), ConstraintSet.LEFT,8);
        constraintSet.connect(mTxtSelectedIdentityHeadline.getId(),ConstraintSet.START,
                mIdentitySelectorLayout.getId(), ConstraintSet.START,8);
        constraintSet.connect(mTxtSelectedIdentityHeadline.getId(),ConstraintSet.RIGHT,
                mIdentitySelectorLayout.getId(), ConstraintSet.RIGHT,8);
        constraintSet.connect(mTxtSelectedIdentityHeadline.getId(),ConstraintSet.END,
                mIdentitySelectorLayout.getId(), ConstraintSet.END,8);
        mTxtSelectedIdentityHeadline.setGravity(Gravity.CENTER | Gravity.BOTTOM);


        constraintSet.connect(mTxtSelectedIdentity.getId(),ConstraintSet.LEFT,
                mIdentitySelectorLayout.getId(), ConstraintSet.LEFT,8);
        constraintSet.connect(mTxtSelectedIdentity.getId(),ConstraintSet.START,
                mIdentitySelectorLayout.getId(), ConstraintSet.START,8);
        constraintSet.connect(mTxtSelectedIdentity.getId(),ConstraintSet.RIGHT,
                mIdentitySelectorLayout.getId(), ConstraintSet.RIGHT,8);
        constraintSet.connect(mTxtSelectedIdentity.getId(),ConstraintSet.END,
                mIdentitySelectorLayout.getId(), ConstraintSet.END,8);
        mTxtSelectedIdentity.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        constraintSet.connect(mTxtSelectedIdentity.getId(),ConstraintSet.TOP,
                mTxtSelectedIdentityHeadline.getId(), ConstraintSet.BOTTOM,4);

        constraintSet.applyTo((ConstraintLayout)mIdentitySelectorLayout);

        mTxtSelectedIdentityHeadline.setTextColor(
                mContext.getResources().getColor(android.R.color.primary_text_dark));

        mTxtSelectedIdentity.setTextColor(
                mContext.getResources().getColor(android.R.color.primary_text_dark));

        mTxtSelectedIdentity.setTextSize(16);
    }

    private void removeIdentity() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    long currentId = SqrlApplication.getCurrentId(mContext);
                    if(currentId != 0) {
                        mDbHelper.deleteIdentity(currentId);
                        SqrlApplication.setCurrentId(mContext, -1);
                        update();
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.main_identity_removed), Toast.LENGTH_SHORT);

                        if(!mDbHelper.hasIdentities()) {
                            mContext.startActivity(new Intent(mContext, StartActivity.class));
                        }
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
        builder.setMessage(R.string.remove_identity_confirmation)
                .setNegativeButton(R.string.remove_identity_confirmation_negative, dialogClickListener)
                .setPositiveButton(R.string.remove_identity_confirmation_positive, dialogClickListener)
                .show();
    }

    private View.OnClickListener mOnListIdentitiesClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            dialogBuilder.setIcon(R.drawable.ic_sqrl_icon_vector_outline);
            dialogBuilder.setTitle(mContext.getResources().getString(R.string.main_selected_identity));
            dialogBuilder.setSingleChoiceItems(mIdentities.values().toArray(new CharSequence[0]),
                    getIndexFromId(mLastId), (DialogInterface dialog, int item) -> {
                        selectIdentityInternal(item);
                        dialog.dismiss();
                    });
            AlertDialog alert = dialogBuilder.create();
            alert.show();
        }
    };

    private View.OnClickListener mOnIdentitySettingsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(mContext, mImgIdentityOptions);
            popup.getMenuInflater()
                    .inflate(R.menu.menu_id_management_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {

                    case R.id.action_idm_settings:
                        mContext.startActivity(new Intent(mContext, SettingsActivity.class));
                        break;
                    case R.id.action_idm_rename:
                        mContext.startActivity(new Intent(mContext, RenameActivity.class));
                        break;
                    case R.id.action_idm_remove:
                        removeIdentity();
                        break;
                    case R.id.action_idm_export:
                        mContext.startActivity(new Intent(mContext, ExportOptionsActivity.class));
                        break;
                    case R.id.action_idm_password_options:
                        showPasswordOptionsMenu();
                        break;
                    default:
                        break;
                }
                return true;
            });

            popup.show();
        }
    };

    private void showPasswordOptionsMenu() {
        PopupMenu popup = new PopupMenu(mContext, mImgIdentityOptions);
        popup.getMenuInflater()
                .inflate(R.menu.menu_id_password_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.action_idm_pw_change_password:
                    mContext.startActivity(new Intent(mContext, ChangePasswordActivity.class));
                    break;
                case R.id.action_idm_pw_reset_password:
                    mContext.startActivity(new Intent(mContext, ResetPasswordActivity.class));
                    break;
                case R.id.action_idm_pw_rekey:
                    mContext.startActivity(new Intent(mContext, RekeyIdentityActivity.class));
                    break;
                default:
                    break;
            }
            return true;
        });

        popup.show();
    }
}