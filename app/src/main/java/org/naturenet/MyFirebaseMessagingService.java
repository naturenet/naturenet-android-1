package org.naturenet;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.naturenet.data.model.Idea;
import org.naturenet.ui.IdeaDetailsActivity;
import org.naturenet.ui.MainActivity;
import org.naturenet.ui.ObservationActivity;

import java.util.Date;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        //make sure there's a logged in user
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            //if the comment was for an Observation
            if(remoteMessage.getData().get("context").equals("observations")){
                Intent intent = new Intent(this, ObservationActivity.class);
                intent.putExtra("observation", remoteMessage.getData().get("parent"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT);

                Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.app_logo)
                        .setContentTitle("NatureNet")
                        .setContentText(remoteMessage.getData().get("body"))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), notificationBuilder.build());
                //if the comment was for an idea
            }else if(remoteMessage.getData().get("context").equals("ideas")){

                //retrieve the idea object
                FirebaseDatabase.getInstance().getReference().child(Idea.NODE_NAME).child(remoteMessage.getData().get("parent")).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Idea idea =dataSnapshot.getValue(Idea.class);

                        goToIdea(idea, remoteMessage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        goToMainActivity(remoteMessage);
                    }
                });

            }
        }

    }

    private void goToIdea(Idea idea, RemoteMessage remoteMessage){

        Intent intent = new Intent(this, IdeaDetailsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("idea", idea);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle("NatureNet")
                .setContentText(remoteMessage.getData().get("body"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), notificationBuilder.build());
    }


    private void goToMainActivity(RemoteMessage remoteMessage){

        Intent intent = new Intent(this,  MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle("NatureNet")
                .setContentText(remoteMessage.getData().get("body"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), notificationBuilder.build());
    }


}
