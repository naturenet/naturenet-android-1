package org.naturenet;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;


public class DeleteTokenService extends IntentService {

    public static final String TAG = DeleteTokenService.class.getSimpleName();

    public DeleteTokenService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        //Here we delete the FirebaseInstanceId which revokes all tokens.
        //Then, call getToken which essentially calls onTokenRefresh() in MyFirebaseInstanceIDService.
        try {

            FirebaseInstanceId.getInstance().deleteInstanceId();

            FirebaseInstanceId.getInstance().getToken();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
