package org.naturenet.ui;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Strings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;

public class LoginFragment extends Fragment {

    public static final String FRAGMENT_TAG = "login_fragment";

    static String SIGNING_IN = "Signing In...";

    DatabaseReference fbRef;
    String email, password;
    LoginActivity log;
    Button signIn;
    TextView join, forgotPassword;
    ProgressDialog pd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        log = ((LoginActivity) this.getActivity());
        signIn = (Button) log.findViewById(R.id.login_b_sign_in);
        join = (TextView) log.findViewById(R.id.login_tv_join);
        forgotPassword = (TextView) log.findViewById(R.id.login_tv_forgot);
        pd = new ProgressDialog(log);
        pd.setMessage(SIGNING_IN);

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = ((EditText) log.findViewById(R.id.login_et_email_address)).getText().toString();
                password = ((EditText) log.findViewById(R.id.login_et_password)).getText().toString();

                if (email.equals("")) {
                    Toast.makeText(log, LoginFragment.this.getResources().getString(R.string.login_error_message_empty_email), Toast.LENGTH_SHORT).show();
                } else if (password.equals("")) {
                    Toast.makeText(log, LoginFragment.this.getResources().getString(R.string.login_error_message_empty_password), Toast.LENGTH_SHORT).show();
                } else if (((NatureNetApplication)getActivity().getApplication()).isConnected()) {
                    signIn.setVisibility(View.GONE);
                    pd.setCancelable(false);
                    pd.show();
                    fbRef = FirebaseDatabase.getInstance().getReference();
                    FirebaseAuth auth = FirebaseAuth.getInstance();

                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(log, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                pd.dismiss();
                                signIn.setVisibility(View.GONE);
                                log.continueAsSignedUser();
                            } else {
                                pd.dismiss();
                                signIn.setVisibility(View.VISIBLE);
                                Toast.makeText(log, LoginFragment.this.getResources().getString(R.string.login_error_message_firebase_login) + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(log, R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*
            When user selects to Join from the login screen.
         */
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((NatureNetApplication)getActivity().getApplication()).isConnected()) {
                    join.setVisibility(View.GONE);
                    log.goToJoinActivity();
                } else {
                    Toast.makeText(log, R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = ((EditText) log.findViewById(R.id.login_et_email_address)).getText().toString();
                if (Strings.isNullOrEmpty(email)) {
                    Toast.makeText(LoginFragment.this.getActivity(), "Please provide the email addressed you used to join NatureNet", Toast.LENGTH_LONG).show();
                } else {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginFragment.this.getActivity(), "Check your email for instructions on resetting your password", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginFragment.this.getActivity(), "There was an error handling your request. Please check the email address and try again.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        join.setVisibility(View.VISIBLE);
    }
}