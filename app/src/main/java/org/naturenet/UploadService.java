package org.naturenet;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
        mObservation = intent.getParcelableExtra(EXTRA_OBSERVATION);
        observationUris = intent.getParcelableArrayListExtra(EXTRA_URI_PATH);
        //initialize targets ArrayList
        targets = new ArrayList<>();
        uploadObservation();
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

            //for each observation uri, we loop and upload the image to either Cloudinary or Firebase
            for(int j=0; j<observationUris.size(); j++){
                final Map<String, String> config = Maps.newHashMap();
                config.put("cloud_name", "university-of-colorado");
                Cloudinary cloudinary = new Cloudinary(config);

                //Try to upload the image to Cloudinary. In case this isn't possible, upload to Firebase Storage.
                try {
                    Map results = cloudinary.uploader().unsignedUpload(getContentResolver().openInputStream(observationUris.get(j)), "android-preset", ObjectUtils.emptyMap());
                    continueWithCloudinaryUpload(results);
                    targets.add(null);
                } catch (IOException ex) {
                    Timber.w(ex, "Failed to upload image to Cloudinary");
                    uploadImageWithFirebase(observationUris.get(j), j);
                }
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

    private void continueWithCloudinaryUpload(Map results) {
        Timber.i("Image uploaded to Cloudinary as %s", results.get("public_id"));
        writeObservationToFirebase((String)results.get("secure_url"));
    }

    private void uploadImageWithFirebase(final Uri uri, final int targetIndex) {
        Timber.i("Attempting upload to Firebase");

        targets.add(new Target() {

            final StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("photos").child(UUID.randomUUID().toString());

            @Override
            public int hashCode() {
                return uri.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return (obj != null) && (obj instanceof Target) && uri.equals(obj);
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Timber.d("Bitmap loaded; uploading data stream to Firebase");
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] data = stream.toByteArray();
                continueWithFirebaseUpload(storageRef.putBytes(data));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Timber.e("Could not load bitmap; uploading original file to Firebase");
                continueWithFirebaseUpload(storageRef.putFile(uri));
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Timber.d("Loading raw bitmap data from %s", uri.getPath());
            }
        });

        if(targets.get(targetIndex) != null){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(UploadService.this)
                            .load(uri)
                            .resize(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                            .centerInside()
                            .onlyScaleDown()
                            .priority(Picasso.Priority.HIGH)
                            .into(targets.get(targetIndex));
                }
            });
        }

    }

    private void continueWithFirebaseUpload(UploadTask uploadTask) {
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                UploadService.this.writeObservationToFirebase(taskSnapshot.getDownloadUrl().toString());
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception ex) {
                        Timber.w(ex, "Image upload task failed: %s", ex.getMessage());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(UploadService.this, getString(R.string.image_upload_error), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    private void writeObservationToFirebase(String imageUrl) {
        final String id = mDatabase.getReference(Observation.NODE_NAME).push().getKey();
        mObservation.id = id;
        mObservation.data.image = imageUrl;
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
    }
}