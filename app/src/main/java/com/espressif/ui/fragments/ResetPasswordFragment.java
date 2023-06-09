// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.fragments;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import com.espressif.AppConstants;
import com.espressif.rainmaker.R;
import com.espressif.ui.user_module.ForgotPasswordActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordFragment extends Fragment {

    private TextView tvResetPasswordMsg;
    private TextInputEditText etPassword, etConfirmPassword;
    private TextInputLayout layoutPassword, layoutCnfPassword;
    private EditText etVerificationCode;
    private MaterialCardView btnSetPassword;
    private TextView txtSetPasswordBtn;
    private ImageView arrowImage;
    private ContentLoadingProgressBar progressBar;
    private String userName;

    public ResetPasswordFragment() {
        // Required empty public constructor
    }

    public static ResetPasswordFragment newInstance() {
        return new ResetPasswordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_reset_password, container, false);

        Bundle extras = getArguments();
        init(rootView, extras);
        return rootView;
    }

    private View.OnClickListener setPasswordBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            getCode();
        }
    };

    private void init(View view, Bundle extras) {

        tvResetPasswordMsg = view.findViewById(R.id.tv_reset_password_msg);
        etPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_new_password);
        etVerificationCode = view.findViewById(R.id.et_verification_code);
        layoutPassword = view.findViewById(R.id.layout_new_password);
        layoutCnfPassword = view.findViewById(R.id.layout_confirm_new_password);
        btnSetPassword = view.findViewById(R.id.btn_set_password);
        txtSetPasswordBtn = view.findViewById(R.id.text_btn);
        arrowImage = view.findViewById(R.id.iv_arrow);
        progressBar = view.findViewById(R.id.progress_indicator);

        if (extras != null) {
            if (extras.containsKey(AppConstants.KEY_USER_NAME)) {
                userName = extras.getString(AppConstants.KEY_USER_NAME);
                String confMsg = getString(R.string.verification_code_sent_instruction) + "<b>" + userName + "</b> ";
                tvResetPasswordMsg.setText(Html.fromHtml(confMsg));
            }
        }

        txtSetPasswordBtn.setText(R.string.btn_set_password);
        btnSetPassword.setOnClickListener(setPasswordBtnClickListener);
    }

    private void getCode() {

        layoutPassword.setError(null);
        layoutCnfPassword.setError(null);

        String newPassword = etPassword.getText().toString();
        if (TextUtils.isEmpty(newPassword)) {

            layoutPassword.setError(getString(R.string.error_password_empty));
            return;
        }

        String newConfirmPassword = etConfirmPassword.getText().toString();
        if (TextUtils.isEmpty(newConfirmPassword)) {

            layoutCnfPassword.setError(getString(R.string.error_confirm_password_empty));
            return;
        }

        if (!newPassword.equals(newConfirmPassword)) {

            layoutCnfPassword.setError(getString(R.string.error_password_not_matched));
            return;
        }

        String verCode = etVerificationCode.getText().toString();
        if (TextUtils.isEmpty(verCode)) {

            etVerificationCode.setError(getString(R.string.error_verification_code_empty));
            return;
        }

        if (!TextUtils.isEmpty(newPassword) && !TextUtils.isEmpty(verCode)) {
            showLoading();
            ((ForgotPasswordActivity) getActivity()).resetPassword(newPassword, verCode);
        }
    }

    private void showLoading() {

        btnSetPassword.setEnabled(false);
        btnSetPassword.setAlpha(0.5f);
        txtSetPasswordBtn.setText(R.string.btn_setting_password);
        progressBar.setVisibility(View.VISIBLE);
        arrowImage.setVisibility(View.GONE);
    }

    public void hideLoading() {

        btnSetPassword.setEnabled(true);
        btnSetPassword.setAlpha(1f);
        txtSetPasswordBtn.setText(R.string.btn_set_password);
        progressBar.setVisibility(View.GONE);
        arrowImage.setVisibility(View.VISIBLE);
    }
}
