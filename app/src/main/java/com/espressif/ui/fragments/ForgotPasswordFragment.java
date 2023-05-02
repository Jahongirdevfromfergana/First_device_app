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
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import com.espressif.AppConstants;
import com.espressif.rainmaker.R;
import com.espressif.ui.Utils;
import com.espressif.ui.user_module.ForgotPasswordActivity;
import com.google.android.material.card.MaterialCardView;

public class ForgotPasswordFragment extends Fragment {

    private EditText etEmail;
    private MaterialCardView btnResetPassword;
    private TextView txtResetPasswordBtn;
    private ContentLoadingProgressBar progressBar;
    private String email;

    public ForgotPasswordFragment() {
        // Required empty public constructor
    }

    public static ForgotPasswordFragment newInstance() {
        return new ForgotPasswordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_forgot_password, container, false);
        Bundle extras = getArguments();
        initViews(rootView, extras);
        return rootView;
    }

    private View.OnClickListener resetPasswordBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            resetPassword();
        }
    };

    private void initViews(View view, Bundle extras) {

        etEmail = view.findViewById(R.id.et_email);
        btnResetPassword = view.findViewById(R.id.btn_reset_password);
        txtResetPasswordBtn = view.findViewById(R.id.text_btn);
        view.findViewById(R.id.iv_arrow).setVisibility(View.GONE);
        progressBar = view.findViewById(R.id.progress_indicator);

        if (extras != null) {
            if (extras.containsKey(AppConstants.KEY_USER_NAME)) {
                this.email = extras.getString(AppConstants.KEY_USER_NAME);
                etEmail.setText(email);
            }
        }

        txtResetPasswordBtn.setText(R.string.btn_reset_password);
        btnResetPassword.setOnClickListener(resetPasswordBtnClickListener);

        etEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_GO) {
                    resetPassword();
                }
                return false;
            }
        });
    }

    private void resetPassword() {

        email = etEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {

            etEmail.setError(getString(R.string.error_username_empty));
            return;

        } else if (!Utils.isValidEmail(email)) {

            etEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        showLoading();
        ((ForgotPasswordActivity) getActivity()).forgotPassword(email);
    }

    private void showLoading() {

        btnResetPassword.setEnabled(false);
        btnResetPassword.setAlpha(0.5f);
        txtResetPasswordBtn.setText(R.string.btn_resetting_password);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {

        btnResetPassword.setEnabled(true);
        btnResetPassword.setAlpha(1f);
        txtResetPasswordBtn.setText(R.string.btn_reset_password);
        progressBar.setVisibility(View.GONE);
    }
}
