
package org.ea.sqrl.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.identity.IdentitySettingsActivity;
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
     * @param hideOnSingleIdentity   Set to true if the identity selector layout should be hidden if only one identity exists.
     */
    public IdentitySelector(Context context, boolean enableIdentityChange,
                            boolean enableIdentityOptions, boolean hideOnSingleIdentity) {
        mContext = context;
        mEnableIdentityChange = enableIdentityChange;
        mEnableIdentityOptions = enableIdentityOptions;
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

        if (mImgListIdentities != null) mImgListIdentities.setOnClickListener(mOnListIdentitiesClickListener);
        if (mImgIdentityOptions != null) mImgIdentityOptions.setOnClickListener(mOnIdentitySettingsClickListener);

        mTxtSelectedIdentityHeadline.setText(
                mTxtSelectedIdentityHeadline.getText() + ":"
        );

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
            if (mImgListIdentities != null) mImgListIdentities.setVisibility(View.GONE);
            mTxtSelectedIdentity.setOnClickListener(null);
        } else {
            if (mImgListIdentities != null) mImgListIdentities.setVisibility(View.VISIBLE);
            mTxtSelectedIdentity.setOnClickListener(mOnListIdentitiesClickListener);
        }

        if (mImgIdentityOptions != null)
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

        String identityName = IdentityDBHelper.getInstance(mContext)
                .getIdentityName(SqrlApplication.getCurrentId(mContext));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
        builder.setTitle(identityName)
                .setMessage(R.string.remove_identity_confirmation)
                .setIcon(R.drawable.ic_delete_menuiconcolor_24dp)
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
            Utils.enablePopupMenuIcons(popup);
            popup.getMenuInflater()
                    .inflate(R.menu.menu_id_management_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {

                    case R.id.action_idm_settings:
                        mContext.startActivity(new Intent(mContext, IdentitySettingsActivity.class));
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
        Utils.enablePopupMenuIcons(popup);
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