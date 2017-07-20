package org.naturenet.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.naturenet.R;
import org.naturenet.data.model.Users;
import org.naturenet.data.model.UsersPrivate;

import timber.log.Timber;

public class JoinActivity extends AppCompatActivity {

    public static final String EXTRA_JOIN = "join";
    public static final String EXTRA_SITE_IDS = "ids";
    public static final String EXTRA_SITE_NAMES = "names";
    public static final String EXTRA_LAUNCH = "launch";
    public static final String EXTRA_LOGIN = "login";
    public static final String EXTRA_NEW_USER = "new_user";

    static final String USERS = "users";
    static final String USERS_PRIVATE = "users-private";
    static final String JOINING = "Joining...";

    String userName, password, name, emailAddress, affiliation, error;
    String[] affiliation_ids, affiliation_names;
    DatabaseReference fbRef;
    EditText et_affiliation;
    ImageButton back;
    Toolbar toolbar;
    Button join;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        et_affiliation = (EditText) findViewById(R.id.join_et_affiliation);
        join = (Button) findViewById(R.id.join_b_join);
        setSupportActionBar(toolbar);
        affiliation_ids = getIntent().getStringArrayExtra(EXTRA_SITE_IDS);
        affiliation_names = getIntent().getStringArrayExtra(EXTRA_SITE_NAMES);
        pd = new ProgressDialog(this);

        et_affiliation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate(JoinActivity.this, R.layout.affiliation_list, null);
                ListView lv_affiliation = (ListView) view.findViewById(R.id.join_lv_affiliation);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, affiliation_names);
                lv_affiliation.setAdapter(adapter);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(view);
                final AlertDialog affiliationList = builder.create();

                lv_affiliation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                        et_affiliation.setText(affiliation_names[position]);
                        affiliation = affiliation_ids[position];
                        affiliationList.dismiss();
                    }
                });

                affiliationList.show();
            }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = ((EditText) JoinActivity.this.findViewById(R.id.join_et_user_name)).getText().toString();
                password = ((EditText) JoinActivity.this.findViewById(R.id.join_et_password)).getText().toString();
                name = ((EditText) JoinActivity.this.findViewById(R.id.join_et_name)).getText().toString();
                emailAddress = ((EditText) JoinActivity.this.findViewById(R.id.join_et_email_address)).getText().toString();

                if (JoinActivity.this.is_any_field_empty() || JoinActivity.this.is_email_address_invalid() || JoinActivity.this.is_password_invalid()) {
                    Toast.makeText(JoinActivity.this, error, Toast.LENGTH_SHORT).show();
                } else {
                    join.setVisibility(View.GONE);
                    pd.setMessage(JOINING);
                    pd.setCancelable(false);
                    pd.show();
                    fbRef = FirebaseDatabase.getInstance().getReference();
                    final FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    Timber.i("Creating new user account for %s", emailAddress);
                    mAuth.createUserWithEmailAndPassword(emailAddress, password)
                            .continueWithTask(new Continuation<AuthResult, Task<Void>>() {
                                @Override
                                public Task<Void> then(@NonNull final Task<AuthResult> taskCreate) throws Exception {
                                    if (taskCreate.isSuccessful()) {
                                        final String id = taskCreate.getResult().getUser().getUid();
                                        Timber.i("Account creation successful for user %s, signing in...", id);
                                        mAuth.signInWithEmailAndPassword(emailAddress, password).addOnCompleteListener(JoinActivity.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    Timber.i("Successful authentication, writing new user data...");
                                                    String default_avatar = getResources().getString(R.string.join_default_avatar);
                                                    final Users user = Users.createNew(id, userName, affiliation, default_avatar);
                                                    final UsersPrivate userPrivate = UsersPrivate.createNew(id, name);
                                                    fbRef.child(USERS).child(id).setValue(user);
                                                    fbRef.child(USERS_PRIVATE).child(id).setValue(userPrivate);
                                                    pd.dismiss();
                                                    join.setVisibility(View.VISIBLE);
                                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.join_success_message), Toast.LENGTH_SHORT).show();
                                                    continueAsSignedUser(user);
                                                } else {
                                                    Timber.e(task.getException(), "Failed to authenticate with new account for %s", id);
                                                    pd.dismiss();
                                                    join.setVisibility(View.VISIBLE);
                                                    Toast.makeText(JoinActivity.this, getResources().getString(R.string.login_error_message_firebase_login) + task.getException(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Timber.e(taskCreate.getException(), "Failed to create account for %s", emailAddress);
                                        pd.dismiss();
                                        join.setVisibility(View.VISIBLE);
                                        Toast.makeText(JoinActivity.this, getResources().getString(R.string.join_error_message_firebase_create) + taskCreate.getException(), Toast.LENGTH_SHORT).show();
                                    }
                                    return null;
                                }
                            });
                }
            }
        });
    }

    private boolean is_any_field_empty() {
        if(userName == null || userName.isEmpty() || password == null || password.isEmpty() ||
                name == null || name.isEmpty() || emailAddress == null || emailAddress.isEmpty() ||
                affiliation == null || affiliation.isEmpty()) {
            error = getResources().getString(R.string.join_error_message_empty);
            return true;
        }
        return false;
    }

    private boolean is_password_invalid() {
        if (password.length() < getResources().getInteger(R.integer.password_min_length)) {
            error = getResources().getString(R.string.join_error_message_password);
            return true;
        }
        return false;
    }

    private boolean is_email_address_invalid() {
        String ePattern = "^.+@.+\\..+$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher matcher = pattern.matcher(emailAddress);

        if (matcher.matches()) { return false; }

        error = getResources().getString(R.string.join_error_message_email_address);
        return true;
    }

    public void clearResources() {
        userName = null;
        password = null;
        name = null;
        emailAddress = null;
        affiliation = null;
        error = null;
        affiliation_ids = null;
        affiliation_names = null;
        fbRef = null;
        et_affiliation = null;
        back = null;
        toolbar = null;
        join = null;
    }

    /*
        This method is called when a user has joined. It sets the result for the Activity activating onActivityResult() in MainActivity and logs the user in.
        Furthermore, it starts MainActivity and clears the Activity backstack.
     */
    public void continueAsSignedUser(Users createdUser) {
        affiliation_ids = null;
        affiliation_names = null;
        //Create intent to set the result of the Activity which effectively logs in the newly created user.
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(EXTRA_JOIN, EXTRA_LOGIN);
        resultIntent.putExtra(EXTRA_NEW_USER, createdUser);
        setResult(Activity.RESULT_OK, resultIntent);
        clearResources();
        //Create intent to start MainActivity. Set flags to clear backstack.
        Intent doneIntent = new Intent(this, MainActivity.class);
        doneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(doneIntent);
        overridePendingTransition(R.anim.stay, R.anim.slide_down);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //handles back button presses on toolbar
                finish();
                overridePendingTransition(R.anim.stay, R.anim.slide_down);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}