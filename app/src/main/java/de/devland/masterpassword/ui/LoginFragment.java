package de.devland.masterpassword.ui;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.lambdaworks.crypto.SCryptUtil;
import com.lyndir.lhunath.opal.system.util.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.devland.esperandro.Esperandro;
import de.devland.masterpassword.R;
import de.devland.masterpassword.base.ui.BaseFragment;
import de.devland.masterpassword.prefs.DefaultPrefs;
import de.devland.masterpassword.util.FingerprintUtil;
import de.devland.masterpassword.util.GenerateUserKeysAsyncTask;
import de.devland.masterpassword.util.Identicon;
import de.devland.masterpassword.util.MasterPasswordHolder;
import de.devland.masterpassword.util.ShowCaseManager;
import lombok.NoArgsConstructor;

/**
 * A simple {@link Fragment} subclass.
 */
@NoArgsConstructor
public class LoginFragment extends BaseFragment {

    @BindView(R.id.editText_masterPassword)
    protected EditText masterPassword;
    @BindView(R.id.editText_fullName)
    protected EditText fullName;
    @BindView(R.id.textView_identicon)
    protected TextView identicon;
    @BindView(R.id.imageView_login)
    protected ImageView loginButton;
    @BindView(R.id.imageView_fingerprint)
    protected ImageView fingerprintIcon;

    protected DefaultPrefs defaultPrefs;

    protected TextWatcher credentialsChangeWatcher = new IdenticonUpdater();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        defaultPrefs = Esperandro.getPreferences(DefaultPrefs.class, getActivity());
        if (!MasterPasswordHolder.INSTANCE.needsLogin(false)) {
            Intent intent = new Intent(getActivity(), PasswordViewActivity.class);
            getActivity().startActivity(intent);
            getActivity().finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            } else {
                fingerprintIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, rootView);
        Drawable wrapped = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.ic_go));
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(getContext(), R.color.login_icon_tint));
        loginButton.setImageDrawable(wrapped);
        wrapped = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.ic_fingerprint_black_24dp));
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(getContext(), R.color.login_icon_tint));
        fingerprintIcon.setImageDrawable(wrapped);
        if (!(defaultPrefs.fingerprintEnabled() && FingerprintUtil.canUseFingerprint(false))) {
            fingerprintIcon.setVisibility(View.GONE);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Typeface typeface = Typeface
                .createFromAsset(getActivity().getAssets(), "fonts/Emoticons-Regular.ttf");
        identicon.setTypeface(typeface);
        fullName.addTextChangedListener(credentialsChangeWatcher);
        masterPassword.addTextChangedListener(credentialsChangeWatcher);
        if (defaultPrefs.saveUserName()) {
            fullName.setText(defaultPrefs.defaultUserName());
        }

        ShowCaseManager.INSTANCE.showLoginShowCase(getActivity(), masterPassword);
    }

    @OnClick(R.id.imageView_login)
    public void onClick() {
        if (checkInputs()) {
            if (defaultPrefs.saveUserName()) {
                defaultPrefs.defaultUserName(fullName.getText().toString());
            }
            GenerateUserKeysAsyncTask keysAsyncTask = new GenerateUserKeysAsyncTask(getActivity(),
                    new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getActivity(), PasswordViewActivity.class);
                            getActivity().startActivity(intent);
                            getActivity().finish();
                        }
                    });
            keysAsyncTask
                    .execute(fullName.getText().toString(), masterPassword.getText().toString());
        }
    }

    private boolean checkInputs() {
        boolean result = true;

        if (masterPassword.getText() == null || masterPassword.getText().toString().equals("")) {
            result = false;
            masterPassword.setError(getActivity().getString(R.string.errorEmpty));
        } else if (fullName.getText() == null || fullName.getText().toString().equals("")) {
            result = false;
            fullName.setError(getActivity().getString(R.string.errorEmpty));
        } else if (defaultPrefs.verifyPassword()) {
            try {
                result = SCryptUtil
                        .check(masterPassword.getText().toString(), defaultPrefs.masterPasswordHash());
                if (!result) {
                    masterPassword.setError(getActivity().getString(R.string.error_incorrectPassword));
                }
            } catch (Exception e) {
                result = false;
                defaultPrefs.masterPasswordHash(null);
                defaultPrefs.verifyPassword(false);
                VerificationFailedDialog verificationFailedDialog = new VerificationFailedDialog();
                verificationFailedDialog.show(getActivity().getSupportFragmentManager(), null);
            }
        }
        return result;
    }

    public class IdenticonUpdater implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String password = masterPassword.getText().toString();
            String name = fullName.getText().toString();
            if (defaultPrefs.showCanary()) {
                if (!StringUtils.isEmpty(password) && !StringUtils.isEmpty(name)) {
                    Identicon mpIdenticon = new Identicon(name, password);
                    identicon.setText(mpIdenticon.getText());
                    int textColor = mpIdenticon.getColor().getColorCode();
                    identicon.setTextColor(textColor);
                } else {
                    identicon.setText("");
                }
            }
        }
    }

    @SuppressLint("ValidFragment")
    public static class VerificationFailedDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(android.R.string.ok, null);

            builder.setTitle(R.string.title_verifyError);
            builder.setMessage(R.string.msg_verifyError);
            Dialog dialog = builder.create();

            return dialog;
        }
    }
}
