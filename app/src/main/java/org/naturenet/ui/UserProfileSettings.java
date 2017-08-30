package org.naturenet.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Lists;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kosalgeek.android.photoutil.CameraPhoto;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.naturenet.R;
import org.naturenet.UploadService;
import org.naturenet.data.model.Users;
import org.naturenet.ui.communities.CommunitiesFragment;
import org.naturenet.ui.communities.UsersDetailActivity;
import org.naturenet.util.CroppedCircleTransformation;
import org.naturenet.util.NatureNetUtils;
import timber.log.Timber;

public class UserProfileSettings extends AppCompatActivity {
    public static final String EXTRA_SITE_IDS = "ids";
    public static final String EXTRA_SITE_NAMES = "names";
    public static final String EXTRA_PROFILE_PIC = "profile_pic";
    private static final int GALLERY_IMAGES = 100;
    private static final int IMAGE_PICKER_RESULTS = 6;
    private static final int REQUEST_CODE_CAMERA = 3;
    private static final int REQUEST_CODE_GALLERY = 4;
    private EditText affiliationSpinner;
    private TextView applyChanges, myAccountButton;
    private EditText bio;
    private Button cameraButton;
    private CameraPhoto cameraPhoto;
    private ImageView cancelPicture;
    private DatabaseReference dbRef;
    private Button galleryButton;
    private GridView gridview;
    private String[] ids;
    private String[] names;
    private Picasso picasso;
    private LinearLayout pictureLayout;
    private ImageView profilePic;
    private List<Uri> recentImageGallery;
    private TextView selectPicture;
    private Uri selectedImage;
    private Users signed_user;
    private ArrayList<String> siteIds;
    private ArrayList<String> siteNames;
    private Toolbar toolbar;
    private EditText username;
    private String affiliation;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_settings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        if (getIntent() != null)
        {
            signed_user = (getIntent().getParcelableExtra("user"));
            ids = getIntent().getStringArrayExtra(EXTRA_SITE_IDS);
            names = getIntent().getStringArrayExtra(EXTRA_SITE_NAMES);
        }

        dbRef = FirebaseDatabase.getInstance().getReference();
        cameraPhoto = new CameraPhoto(this);
        picasso = Picasso.with(this);
        picasso.setIndicatorsEnabled(false);
        applyChanges = ((TextView)findViewById(R.id.profile_settings_apply));
        profilePic = ((ImageView)findViewById(R.id.profile_settings_user_pic));
        bio = ((EditText)findViewById(R.id.profile_settings_bio));
        username = ((EditText)findViewById(R.id.profile_settings_username));
        affiliationSpinner = ((EditText) findViewById(R.id.profile_settings_affiliation_spinner));
        pictureLayout = ((LinearLayout)findViewById(R.id.ll_dialog_add_observation));
        cancelPicture = (ImageView) findViewById(R.id.dialog_add_observation_iv_cancel);
        selectPicture = ((TextView)findViewById(R.id.dialog_add_observation_tv_select));
        cameraButton = ((Button)findViewById(R.id.dialog_add_observation_b_camera));
        galleryButton = ((Button)findViewById(R.id.dialog_add_observation_b_gallery));
        gridview = ((GridView)findViewById(R.id.dialog_add_observation_gv));
        myAccountButton = (TextView) findViewById(R.id.myAccountButton);
        pictureLayout.setVisibility(View.GONE);


        dbRef.child(Users.NODE_NAME).child(signed_user.id).addListenerForSingleValueEvent(new ValueEventListener()
        {
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                signed_user = dataSnapshot.getValue(Users.class);
                setCurrentValues();
            }

            public void onCancelled(DatabaseError databaseError) {

            }

        });

        profilePic.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (ContextCompat.checkSelfPermission(UserProfileSettings.this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                    ActivityCompat.requestPermissions(UserProfileSettings.this, new String[] { "android.permission.WRITE_EXTERNAL_STORAGE" }, 4);
                }else {
                    selectPicture.setVisibility(View.GONE);
                    pictureLayout.setVisibility(View.VISIBLE);
                    setGallery();
                }
            }
        });

        affiliationSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate(UserProfileSettings.this, R.layout.affiliation_list, null);
                ListView lv_affiliation = (ListView) view.findViewById(R.id.join_lv_affiliation);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, names);
                lv_affiliation.setAdapter(adapter);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(view);
                final AlertDialog affiliationList = builder.create();

                lv_affiliation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                        affiliationSpinner.setText(names[position]);
                        affiliation = ids[position];
                        affiliationList.dismiss();
                    }
                });

                affiliationList.show();
            }
        });

        /**
         * Listener for when user selects 'My Account' button.
         */
        myAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userIntent = new Intent(UserProfileSettings.this, UsersDetailActivity.class);
                userIntent.putExtra(CommunitiesFragment.USER_EXTRA, signed_user);

                //Make sure current user isn't null (not signed in/lost signal)
                if(signed_user!=null){
                    startActivity(userIntent);
                }else
                    Toast.makeText(UserProfileSettings.this, "Could not load account", Toast.LENGTH_SHORT).show();
            }
        });

        cancelPicture.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                pictureLayout.setVisibility(View.GONE);
                selectedImage = null;
            }
        });


        selectPicture.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                NatureNetUtils.showImage(getApplicationContext(), profilePic, selectedImage, true, false);
                pictureLayout.setVisibility(View.GONE);
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (isStoragePermitted())
                {
                    setGallery();
                    selectPicture.setVisibility(View.GONE);

                    try {
                        startActivityForResult(cameraPhoto.takePhotoIntent(), REQUEST_CODE_CAMERA);
                    } catch (IOException e) {
                        Toast.makeText(UserProfileSettings.this, "Something Wrong while taking photo", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(UserProfileSettings.this, R.string.permission_rejected, Toast.LENGTH_LONG).show();
                }

            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (isStoragePermitted())
                {
                    setGallery();
                    selectPicture.setVisibility(View.GONE);
                    selectedImage = null;

                    if (usingApiEighteenAndAbove())
                    {
                        Intent galleryIntent = new Intent();
                        galleryIntent.setType("image/*");
                        galleryIntent.setAction("android.intent.action.GET_CONTENT");
                        startActivityForResult(Intent.createChooser(galleryIntent,"Select Picture"), GALLERY_IMAGES);
                    }else{
                        Intent imagePickerIntent = new Intent(getApplicationContext(), ImagePicker.class);
                        imagePickerIntent.putExtra(EXTRA_PROFILE_PIC, true);
                        startActivityForResult(imagePickerIntent, IMAGE_PICKER_RESULTS);
                    }

                }else
                    Toast.makeText(UserProfileSettings.this, R.string.permission_rejected, Toast.LENGTH_LONG).show();

            }
        });

        applyChanges.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                //make sure username text isn't blank
                if (username.getText().toString().length() != 0) {
                    applyChanges.setVisibility(View.GONE);
                    dialog = ProgressDialog.show(UserProfileSettings.this, "", "Updating", true, false);
                    dialog.show();
                    //if they actually selected a different image
                    if (selectedImage != null) {
                        Intent uploadIntent = new Intent(UserProfileSettings.this, UploadService.class);
                        uploadIntent.putExtra(EXTRA_PROFILE_PIC, selectedImage);
                        uploadIntent.putExtra("id", signed_user.id);
                        startService(uploadIntent);
                        setUpdatedValues(bio.getText().toString(), username.getText().toString(), affiliation);
                    }else
                        setUpdatedValues(bio.getText().toString(), username.getText().toString(), affiliation);
                }else
                    Toast.makeText(UserProfileSettings.this, "Username cannot be blank.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case GALLERY_IMAGES:
                if(resultCode == RESULT_OK){
                    if(data!=null){
                        selectedImage = data.getData();
                        NatureNetUtils.showImage(this, profilePic, selectedImage, true, false);
                        pictureLayout.setVisibility(View.GONE);
                    }
                }
                break;
            case REQUEST_CODE_CAMERA:
                if(resultCode == RESULT_OK){
                    selectedImage = Uri.fromFile(new File(cameraPhoto.getPhotoPath()));
                    NatureNetUtils.showImage(this, profilePic, selectedImage, true, true);
                    pictureLayout.setVisibility(View.GONE);
                }
                break;
            case IMAGE_PICKER_RESULTS:
                if(resultCode == RESULT_OK){
                    ArrayList<Uri> images = data.getParcelableArrayListExtra("images");
                    selectedImage = images.get(0);
                    NatureNetUtils.showImage(this, profilePic, selectedImage, true, false);
                    pictureLayout.setVisibility(View.GONE);
                }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down);
    }

    /**
     * This method sets the current values of the signed in user.
     */
    private void setCurrentValues()
    {
        bio.setText(signed_user.bio);
        username.setText(signed_user.displayName);
        if ((signed_user.avatar != null) && (signed_user.avatar.length() > 0)) {
            picasso.load(signed_user.avatar).transform(new CroppedCircleTransformation()).noFade().placeholder(R.drawable.default_avatar).into(this.profilePic);
        }
        siteIds = new ArrayList<>(Arrays.asList(ids));
        siteNames = new ArrayList<>(Arrays.asList(names));
        affiliationSpinner.setText(siteNames.get(siteIds.indexOf(signed_user.affiliation)));
        affiliation = signed_user.affiliation;
    }

    public List<Uri> getRecentImagesUris() {
        Uri uri;
        Cursor cursor;
        List<Uri> listOfAllImages = Lists.newArrayList();
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN };
        cursor = this.getContentResolver().query(uri, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                do {
                    listOfAllImages.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                            new File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))));
                } while (cursor.moveToNext() && listOfAllImages.size() < 8);
            } catch (CursorIndexOutOfBoundsException ex) {
                Timber.e(ex, "Could not read data from MediaStore, image gallery may be empty");
            } finally {
                cursor.close();
            }
        }else {
            Timber.e("Could not get MediaStore content!");
        }
        return listOfAllImages;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    setGallery();
                else
                    Toast.makeText(this, "Gallery Access Permission Denied", Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    setGallery();
                else
                    Toast.makeText(this, "Camera Access Permission Denied", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    public void setGallery() {

        recentImageGallery = getRecentImagesUris();

        if (recentImageGallery.size() != 0) {
            gridview.setAdapter(new ImageGalleryAdapter(this, recentImageGallery));

            //Here we handle clicks to the recent images. Let user select as many images as they want to submit.
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    ImageView iv = (ImageView) v.findViewById(R.id.gallery_iv);

                    //if the user hasn't selected an image yet
                    if (selectedImage == null) {
                        //set the selected image
                        selectedImage = recentImageGallery.get(position);
                        iv.setBackground(ContextCompat.getDrawable(UserProfileSettings.this, R.drawable.border_selected_image));
                        selectPicture.setVisibility(View.VISIBLE);
                        //here we handle the case of selecting an image that's already been selected
                    } else if (selectedImage == recentImageGallery.get(position)) {
                        selectedImage = null;
                        iv.setBackgroundResource(0);
                        selectPicture.setVisibility(View.GONE);
                    } else {
                        //get index of item that was previously selected
                        int i = recentImageGallery.indexOf(selectedImage);

                        //set that item as unselected on the UI
                        if (i > 0) {
                            gridview.getChildAt(i).findViewById(R.id.gallery_iv).setBackgroundResource(0);
                        }

                        //now set the newly selected picture
                        selectedImage = recentImageGallery.get(position);
                        iv.setBackground(ContextCompat.getDrawable(UserProfileSettings.this, R.drawable.border_selected_image));
                        selectPicture.setVisibility(View.VISIBLE);
                    }
                }
            });

        }
    }

    public boolean usingApiEighteenAndAbove()
    {
        return Build.VERSION.SDK_INT >= 18;
    }

    private boolean isStoragePermitted(){

        boolean isPermissionGiven = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                isPermissionGiven = true;
            }
        }

        return isPermissionGiven;
    }

    private void setUpdatedValues(String bio, String username, String affiliation)
    {
        signed_user.bio = bio;
        signed_user.displayName = username;
        signed_user.affiliation = affiliation;

        dbRef.child(Users.NODE_NAME).child(signed_user.id).setValue(signed_user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(UserProfileSettings.this, "Profile updated.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                finish();
            }
        });
    }
}

