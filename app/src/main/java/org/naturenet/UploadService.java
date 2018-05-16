package org.naturenet;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;
import android.content.Context;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Users;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import timber.log.Timber;

public class UploadService extends IntentService {

    public static final String EXTRA_URI_PATH = "uri_path";
    public static final String EXTRA_OBSERVATION = "observation";

    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final String LATEST_CONTRIBUTION = "latest_contribution";

    private FirebaseDatabase mDatabase;
    private Observation mObservation;
    private ArrayList<Target> targets;
    private ArrayList<Uri> observationUris;
    private Handler mHandler = new Handler();
    private Uri profilePicUri;
    private String userId;
    private boolean isProfilePic = false;

    public UploadService() {
        super("UploadService");
        mDatabase = FirebaseDatabase.getInstance();
    }

    /**
     * Here we retrieve the data that was passed through the intent.
     * @param intent
     */
    @Override
    public void onHandleIntent(Intent intent) {

        //check to see if we're getting an observation
        if(intent.getParcelableExtra(EXTRA_OBSERVATION) != null){
            mObservation = intent.getParcelableExtra(EXTRA_OBSERVATION);
            observationUris = intent.getParcelableArrayListExtra(EXTRA_URI_PATH);
            //initialize targets ArrayList
            targets = new ArrayList<>();
            uploadObservation();
        }
    }

    private void uploadObservation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(mObservation.userId)) {
            Timber.d("Preparing image for upload");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UploadService.this, "Your observation is being submitted.", Toast.LENGTH_SHORT).show();
                }
            });

            Map<String, String> observationImages = Maps.newHashMap();
            for(int j=0; j<observationUris.size(); j++) {
                String oid = writeObservationToFirebase(observationUris.get(j));
                observationImages.put(oid, observationUris.get(j).toString());
            }

            // append observationImages to file
            appendToObservationImages(observationImages);

            if (NatureNetApplication.isConnected()) {
                UploadRemainingImages();
            }

        } else {
            Timber.w("Attempt to upload observation without valid login");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UploadService.this, "Please sign in to contribute an observation.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String writeObservationToFirebase(Uri localPath) {
        if(!isProfilePic){
            final String id = mDatabase.getReference(Observation.NODE_NAME).push().getKey();
            mObservation.id = id;
            mObservation.data.image = localPath.getLastPathSegment();
            mDatabase.getReference(Observation.NODE_NAME).child(id).setValue(mObservation, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Timber.w(databaseError.toException(), "Failed to write observation to database: %s", databaseError.getMessage());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UploadService.this, getString(R.string.dialog_add_observation_error), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mDatabase.getReference(Users.NODE_NAME).child(mObservation.userId).child(LATEST_CONTRIBUTION).setValue(ServerValue.TIMESTAMP);
                        mDatabase.getReference(Project.NODE_NAME).child(mObservation.projectId).child(LATEST_CONTRIBUTION).setValue(ServerValue.TIMESTAMP);
                        new GeoFire(mDatabase.getReference("geo")).setLocation(mObservation.id, new GeoLocation(mObservation.getLatitude(), mObservation.getLongitude()));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UploadService.this, getString(R.string.dialog_add_observation_success), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            return id;
        }
        return "";
    }

    public static void UploadRemainingImages() {
        // read the list of remaining images
        Map<String, String> observationImages = readObservationImages();
        // for each remaining image try to upload to cloudinary; if successful remove it from the list
        Object[] ids = observationImages.keySet().toArray();
        if (ids.length == 0) { return; }
        for (int i = 0;i<ids.length;i++) {
            if (uploadToCloudinary((String)ids[i], observationImages.get(ids[i]))) {
                observationImages.remove(ids[i]);
            }
        }
        // write the new list of observation images to file
        writeObservationImages(observationImages);
    }

    private static boolean uploadToCloudinary(String observationId, String observationImage) {
        final Map<String, String> config = Maps.newHashMap();
        config.put("cloud_name", "university-of-colorado");
        Cloudinary cloudinary = new Cloudinary(config);

        try {
            Map results = cloudinary.uploader().unsignedUpload(NatureNetApplication.getAppContext().getContentResolver().openInputStream(Uri.parse(observationImage)),
                    "android-preset", ObjectUtils.emptyMap());
            if (results.containsKey("secure_url")) {
                updateObservationImageUrl(observationId, (String)results.get("secure_url"));
                return true;
            }
        } catch (IOException ex) {
            Timber.w(ex, "Failed to upload image to Cloudinary");
        }
        return false;
    }

    private static void updateObservationImageUrl(String observationId, String url) {
        DatabaseReference mRef = FirebaseDatabase.getInstance().getReference(Observation.NODE_NAME).child(observationId);
        mRef.child("data").child("image").setValue(url).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(!task.isSuccessful()){
                    //Toast.makeText(o, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static Map<String, String> readObservationImages() {
        Map<String, String> observationImages = Maps.newHashMap();
        try {
            FileInputStream fis =  NatureNetApplication.getAppContext().openFileInput("observationImages");
            ObjectInputStream is = new ObjectInputStream(fis);
            observationImages = (Map<String, String>) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return observationImages;
    }

    private static void writeObservationImages(Map<String, String> observationImages) {
        try {
            FileOutputStream fos = NatureNetApplication.getAppContext().openFileOutput("observationImages", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(observationImages);
            os.close();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void appendToObservationImages(Map<String, String> observationImages) {
        Map<String, String> currentObsImgs = readObservationImages();
        currentObsImgs.putAll(observationImages);
        writeObservationImages(currentObsImgs);
    }
}
