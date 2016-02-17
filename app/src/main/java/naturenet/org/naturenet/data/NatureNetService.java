package naturenet.org.naturenet.data;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import naturenet.org.naturenet.BuildConfig;
import naturenet.org.naturenet.R;

public class NatureNetService extends IntentService {

    public static final String ACTION_LOGIN = "LOGIN";

    private Handler mHandler;
    private SharedPreferences mPreferences;
    private String mLogin;
    private String mCredentials;

    private Firebase mFirebase = new Firebase(BuildConfig.FIREBASE_ROOT_URL);

    public NatureNetService() {
        super("NatureNetService");
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mPreferences = getSharedPreferences(getString(R.string.naturenet_prefs), MODE_PRIVATE);
        mLogin = mPreferences.getString("login", null);
        mCredentials = mPreferences.getString("credentials", null);

        //TODO
        String action = intent.getAction();
        if(action.equals(ACTION_LOGIN)) {

        }
    }

    private void verifyCredentials() {
        Firebase.AuthResultHandler authHandler = new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                //TODO: proceed
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                //TODO: report error
            }
        };

        mFirebase.authWithPassword(mLogin, mCredentials, authHandler);
    }

    private void logout() {
        mFirebase.unauth();
    }
}
