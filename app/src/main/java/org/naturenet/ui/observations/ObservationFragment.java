package org.naturenet.ui.observations;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.naturenet.DownloadFile;
import org.naturenet.R;
import org.naturenet.data.model.Comment;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.Users;
import org.naturenet.ui.CommentsAdapter;
import org.naturenet.ui.LoginActivity;
import org.naturenet.util.NatureNetUtils;

import timber.log.Timber;

public class ObservationFragment extends Fragment {

    public static final String FRAGMENT_TAG = "observation_fragment";
    private static final String ARG_OBSERVATION = "ARG_OBSERVATION";
    private static final int REQUEST_PDF_SAVE = 200;
    private static final int REQUEST_IMAGE_SAVE = 100;

    boolean isImageFitToScreen;

    String observationLink;
    Boolean filePermission = false;
    ObservationActivity o;
    Observation observation;
    ImageView observer_avatar, observation_image, like;
    TextView observer_name, observer_affiliation, observation_timestamp, observation_text, send, editButton, deleteButton;
    RelativeLayout commentLayout;
    LinearLayout observer_layout;
    EditText comment;
    int height, width;
    ListView lv_comments;
    LinearLayout comment_view;
    private String mObservationId;
    ViewGroup.LayoutParams params;
    private DatabaseReference mRef;
    private final ValueEventListener mObservationListener = new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            observation = dataSnapshot.getValue(Observation.class);

            if(observation == null) {
                Timber.e("Observation %s does not exist!", mObservationId);
                return;
            }

            Picasso.with(getActivity())
                    .load(Strings.emptyToNull(observation.data.image))
                    .placeholder(R.drawable.default_image)
                    .error(R.drawable.no_image)
                    .fit()
                    .centerInside()
                    .into(observation_image);

            if (observation.data.text != null) {
                observation_text.setText(observation.data.text);
            } else {
                observation_text.setText(R.string.no_description);
            }

            if(o.signed_user == null || !observation.userId.equals(o.signed_user.id)){
                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
            }

            observation_timestamp.setText(NatureNetUtils.toDateString(observation));

            FirebaseDatabase.getInstance().getReference(Project.NODE_NAME).child(observation.projectId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Project project = dataSnapshot.getValue(Project.class);
                            if(project != null) {
                                o.getSupportActionBar().setTitle(project.name);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Timber.w(databaseError.toException(), "Unable to read data for project %s", observation.projectId);
                        }
                    });

            FirebaseDatabase.getInstance().getReference(Users.NODE_NAME).child(observation.userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final Users user = dataSnapshot.getValue(Users.class);

                            if(user == null) {
                                Timber.e("User %s does not exist!", observation.userId);
                                return;
                            }

                            NatureNetUtils.showUserAvatar(getActivity(), observer_avatar, user.avatar);
                            if (user.displayName != null) {
                                observer_name.setText(user.displayName);
                            } else {
                                observer_name.setText(R.string.unknown_user);
                            }

                            FirebaseDatabase.getInstance().getReference(Site.NODE_NAME).child(user.affiliation)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Site site = dataSnapshot.getValue(Site.class);
                                            if (site.name != null) {
                                                observer_affiliation.setText(site.name);
                                            } else {
                                                observer_affiliation.setText(null);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Timber.w(databaseError.toException(), "Unable to read data for site %s", user.affiliation);
                                        }
                                    });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Timber.w(databaseError.toException(), "Unable to read data for user %s", observation.userId);
                        }
                    });

            if (o.signed_user != null && observation.likes != null
                    && observation.likes.containsKey(o.signed_user.id) && observation.likes.get(o.signed_user.id)) {
                like.setImageDrawable(ContextCompat.getDrawable(ObservationFragment.this.getActivity(), R.drawable.like));
            }

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Timber.w(databaseError.toException(), "Unable to read data for observation %s", mObservationId);
        }
    };

    public static ObservationFragment newInstance(String observationId) {
        ObservationFragment frag = new ObservationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OBSERVATION, observationId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_observation, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        observer_avatar = (ImageView) view.findViewById(R.id.selected_observer_avatar);
        observation_image = (ImageView) view.findViewById(R.id.selected_observation_icon);
        params = observation_image.getLayoutParams();
        like = (ImageView) view.findViewById(R.id.iv_like);
        observer_name = (TextView) view.findViewById(R.id.selected_observer_user_name);
        observer_affiliation = (TextView) view.findViewById(R.id.selected_observer_affiliation);
        observation_timestamp = (TextView) view.findViewById(R.id.selected_observeration_timestamp);
        observation_text = (TextView) view.findViewById(R.id.selected_observation_text);
        send = (TextView) view.findViewById(R.id.tv_send);
        comment = (EditText) view.findViewById(R.id.et_comment);
        lv_comments = (ListView) view.findViewById(R.id.lv_comments);
        commentLayout = (RelativeLayout) view.findViewById(R.id.rl_comment);
        comment_view = (LinearLayout) view.findViewById(R.id.scroll_view);
        observer_layout = (LinearLayout) view.findViewById(R.id.ll_observer);
        editButton = (TextView) view.findViewById(R.id.editObsButton);
        deleteButton = (TextView) view.findViewById(R.id.deleteObsButton);
    }

    //called when activity is changed to landscape mode
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            observation_image.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            observation_image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            observation_text.setVisibility(View.GONE);
            if(isAboveKitKat()){
                getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }else{
                getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() == null || getArguments().getString(ARG_OBSERVATION) == null) {
            Timber.e(new IllegalArgumentException(), "Tried to load ObservationFragment without an Observation argument");
            Toast.makeText(getActivity(), "No observation to display", Toast.LENGTH_SHORT).show();
            return;
        }

        o = (ObservationActivity) getActivity();
        mObservationId = getArguments().getString(ARG_OBSERVATION);

        getComments(mObservationId);

        mRef = FirebaseDatabase.getInstance().getReference(Observation.NODE_NAME).child(mObservationId);
        mRef.addValueEventListener(mObservationListener);

        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (o.signed_user != null) {
                    Optional<Boolean> value;

                    if (observation.likes != null && observation.likes.containsKey(o.signed_user.id) && observation.likes.get(o.signed_user.id)) {
                        like.setImageDrawable(ContextCompat.getDrawable(ObservationFragment.this.getActivity(), R.drawable.likes));
                        value = Optional.absent();
                    } else {
                        like.setImageDrawable(ContextCompat.getDrawable(ObservationFragment.this.getActivity(), R.drawable.like));
                        value = Optional.of(true);
                    }

                    DatabaseReference fbRef = FirebaseDatabase.getInstance().getReference();
                    fbRef.child("observations").child(observation.id).child("likes").child(o.signed_user.id).setValue(value.orNull());
                } else {
                    Toast.makeText(getActivity(), "Please login to like an observation.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        observation_image.setOnLongClickListener(new View.OnLongClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public boolean onLongClick(View v) {

                observationLink = observation.data.image;

                String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};

                //check to see if the observation is a pdf or image
                if(observationLink.contains("pdf")){
                    //if above Android 6.0 Marshmallow, ask for permission to save file to memory
                    if(canMakeSmores()){
                        requestPermissions(perms, REQUEST_PDF_SAVE);
                    }else{  //otherwise, simply continue with the download
                        new AlertDialog.Builder(getActivity()).setTitle("Save")
                                .setMessage("Would you like to download the pdf?")
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        DownloadFile download = new DownloadFile(getActivity(), observation.id);
                                        download.execute(observationLink.substring(0, observationLink.length()-4) + ".pdf");
                                    }
                                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing if rejected
                            }
                        }).show();
                    }
                }else{  //in this case it's an image
                    //check to see if user has given us the permission to save images OR if Android version < Marshmallow, simply continue
                    if(isStoragePermitted() || !canMakeSmores()){   //if they have, we continue with saving the image
                        new AlertDialog.Builder(getActivity()).setTitle("Save")
                                .setMessage("Save image to gallery?")
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        BitmapDrawable bmd = (BitmapDrawable) observation_image.getDrawable();
                                        Bitmap bd = bmd.getBitmap();

                                        String isSaved = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bd, observation.projectId , observation.data.text);
                                        if(isSaved==null){
                                            Toast.makeText(getActivity(), "Image could not be saved.", Toast.LENGTH_SHORT).show();
                                        }else
                                            Toast.makeText(getActivity(), "Image saved to gallery!", Toast.LENGTH_SHORT).show();

                                    }
                                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing if rejected
                            }
                        }).show();
                    }else{  //if they haven't given permission, ask for permission
                        requestPermissions(perms, 100);
                    }
                }

                return false;
            }
        });


        //single click on observation image will zoom the screen
        observation_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the screen is in full-screen mode
                if(isImageFitToScreen) {
                    //reset the screen to normal
                    isImageFitToScreen=false;
                    observation_image.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                    observation_image.setAdjustViewBounds(true);
                    observation_image.setLayoutParams(params);
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    o.getSupportActionBar().show();
                    commentLayout.setVisibility(View.VISIBLE);
                    comment_view.setVisibility(View.VISIBLE);
                    observer_layout.setVisibility(View.VISIBLE);
                    observation_text.setVisibility(View.VISIBLE);
                    getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }else{  //otherwise, remove all elements from screen except the imageView
                    isImageFitToScreen=true;
                    o.getSupportActionBar().hide();
                    commentLayout.setVisibility(View.GONE);
                    comment_view.setVisibility(View.GONE);
                    observer_layout.setVisibility(View.GONE);
                    height = observation_image.getDrawable().getIntrinsicHeight();
                    width = observation_image.getDrawable().getIntrinsicWidth();

                    //landscape image
                    if(width>height){
                        //flip the orientation, this will trigger onConfigurationChanged() ^^^
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }else{  //portrait image
                        observation_image.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                        observation_image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        observation_text.setVisibility(View.GONE);
                        if(isAboveKitKat()){
                            getActivity().getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
                        }else{
                            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                        }

                    }

                }
            }
        });

        //Handle edit button clicks.
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder editPopup = new AlertDialog.Builder(getActivity());
                editPopup.setTitle("Edit Caption");

                final EditText text = new EditText(getActivity());
                text.setSingleLine();
                text.setText(observation.data.text);
                editPopup.setView(text);

                editPopup.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Update the caption in the databsae. Should reflect automatically on the UI because we've set a ValueListener which automatically syncs on data change.
                        mRef.child("data").child("text").setValue(text.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(!task.isSuccessful()){
                                    Toast.makeText(o, "Something went wrong", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                editPopup.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                editPopup.show();
            }
        });

        //Handle delete button clicks.
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder deletePopup = new AlertDialog.Builder(getActivity());
                deletePopup.setTitle("Delete");
                deletePopup.setMessage("Are you sure you want to delete your Observation?");

                deletePopup.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mRef.child("status").setValue("deleted").addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(o, "Observation deleted", Toast.LENGTH_SHORT).show();
                                    o.finish();
                                }else
                                    Toast.makeText(o, "Could not delete observation", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                deletePopup.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                deletePopup.show();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commentText = comment.getText().toString();

                if (!commentText.isEmpty()) {
                    if (o.signed_user != null) {
                        send.setEnabled(false);
                        comment.setEnabled(false);
                        final DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference().child(Comment.NODE_NAME).push();
                        Comment newComment = Comment.createNew(commentRef.getKey(), commentText, o.signed_user.id, observation.id, Observation.NODE_NAME);

                        commentRef.setValue(newComment, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                send.setEnabled(true);
                                comment.setEnabled(true);

                                if (databaseError != null) {
                                    Timber.w("Could not write comment for %s: %s", observation.id, databaseError.getDetails());
                                    Toast.makeText(getActivity(), "Your comment could not be submitted.", Toast.LENGTH_LONG).show();
                                } else {
                                    // Update /observations/<observation-id>/comments/ with new comment
                                    FirebaseDatabase.getInstance().getReference().child(Observation.NODE_NAME)
                                            .child(observation.id).child("comments").child(commentRef.getKey()).setValue(true);
                                    comment.getText().clear();
                                    Toast.makeText(getActivity(), "Your comment has been submitted.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(getActivity(), "Please login to comment.", Toast.LENGTH_SHORT).show();
                        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                        startActivityForResult(loginIntent, 2);
                    }
                }
            }
        });
    }

    public void getComments(String observation){

        Query query = FirebaseDatabase.getInstance().getReference().child(Comment.NODE_NAME).orderByChild("parent").equalTo(observation);
        CommentsAdapter adapter = new CommentsAdapter(getActivity(), query);

        lv_comments.setAdapter(adapter);

    }

    @Override
    public void onDestroyView() {
        mRef.removeEventListener(mObservationListener);
        super.onDestroyView();
    }

    /*
        This method is called when the user either gives or rejects specific permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PDF_SAVE:
                filePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                //if user gives permission to save pdf
                if(filePermission) {
                    new AlertDialog.Builder(getActivity()).setTitle("Save")
                            .setMessage("Would you like to download the pdf?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    DownloadFile download = new DownloadFile(getActivity(), observation.id);
                                    download.execute(observationLink.substring(0, observationLink.length()-4) + ".pdf");
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //do nothing if rejected
                        }
                    }).show();
                }
                break;
            case REQUEST_IMAGE_SAVE:
                filePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                //if user has given permission to save image
                if(filePermission){
                    new AlertDialog.Builder(getActivity()).setTitle("Save")
                            .setMessage("Save image to gallery?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    BitmapDrawable bmd = (BitmapDrawable) observation_image.getDrawable();
                                    Bitmap bd = bmd.getBitmap();

                                    String isSaved = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bd, observation.projectId , observation.data.text);
                                    if(isSaved==null){
                                        Toast.makeText(getActivity(), "Image could not be saved.", Toast.LENGTH_SHORT).show();
                                    }else
                                        Toast.makeText(getActivity(), "Image saved to gallery!", Toast.LENGTH_SHORT).show();

                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //do nothing if rejected
                        }
                    }).show();
                }
                break;
        }
    }

    private boolean canMakeSmores(){
        return(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    private boolean isAboveKitKat(){
        return(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT);
    }

    /*
        This method checks to see if the user has already given permission to write to storage.
     */
    private boolean isStoragePermitted(){

        boolean isPermissionGiven = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                isPermissionGiven = true;
            }
        }

        return isPermissionGiven;
    }
}