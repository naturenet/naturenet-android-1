package org.naturenet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.naturenet.data.model.Users;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TOKEN_NODE = "notification_token";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        //get the token string and current user
        String tokenId = FirebaseInstanceId.getInstance().getToken();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        //if current user isn't null
        if(firebaseUser != null){
            //push the token to the user's node
            FirebaseDatabase.getInstance().getReference()
                    .child(Users.NODE_NAME)
                    .child(firebaseUser.getUid())
                    .child(TOKEN_NODE)
                    .setValue(tokenId);
        }

    }
}
