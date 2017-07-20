package org.naturenet.ui;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kosalgeek.android.photoutil.CameraPhoto;
import com.kosalgeek.android.photoutil.GalleryPhoto;
import com.squareup.picasso.Picasso;

import org.naturenet.NatureNetApplication;
import org.naturenet.R;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.Users;
import org.naturenet.util.NatureNetUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final static int REQUEST_CODE_JOIN = 1;
    private final static int REQUEST_CODE_LOGIN = 2;

    static String NAME = "name";

    String[] affiliation_ids, affiliation_names;
    Observation previewSelectedObservation;
    List<String> ids, names;
    DatabaseReference mFirebase;
    Users signed_user;
    Site user_home_site;
    DrawerLayout drawer;
    Toolbar toolbar;
    NavigationView navigationView;
    View header;
    Button sign_in, join;
    TextView display_name, affiliation, licenses;
    ImageView nav_iv;
    MenuItem logout;
    private Disposable mUserAuthSubscription;
    int pastSelection = 0;
    int currentSelection =0;
    Stack<Integer> selectionStack;
    ArrayList<Users> userList;

    /* Common submission items */
    static final private int REQUEST_CODE_CAMERA = 3;
    static final private int REQUEST_CODE_GALLERY = 4;
    static final private int REQUEST_CODE_CHECK_LOCATION_SETTINGS = 5;
    static final private int IMAGE_PICKER_RESULTS = 6;
    static final private int GALLERY_IMAGES = 100;
    CameraPhoto cameraPhoto;
    GalleryPhoto galleryPhoto;
    Uri observationPath;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Button camera, gallery;
    TextView select;
    LinearLayout dialog_add_observation;
    GridView gridview;
    ImageView add_observation_cancel, gallery_item, add_observation_button;
    List<Uri> recentImageGallery, observationPaths;
    ArrayList<Uri> selectedImages;
    double latValue, longValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userList = FetchData.getInstance().getUsers();
        setContentView(R.layout.activity_main);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        logout = navigationView.getMenu().findItem(R.id.nav_logout);
        header = navigationView.getHeaderView(0);
        sign_in = (Button) header.findViewById(R.id.nav_b_sign_in);
        join = (Button) header.findViewById(R.id.nav_b_join);
        nav_iv = (ImageView) header.findViewById(R.id.nav_iv);
        display_name = (TextView) header.findViewById(R.id.nav_tv_display_name);
        affiliation = (TextView) header.findViewById(R.id.nav_tv_affiliation);
        licenses = (TextView) navigationView.findViewById(R.id.licenses);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        add_observation_button = (ImageView) findViewById(R.id.addObsButton);
        selectionStack = new Stack<>();
        selectedImages = new ArrayList<>();

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);


        licenses.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(v.getContext())
                            .setView(View.inflate(MainActivity.this, R.layout.about, null))
                            .setNegativeButton("Dismiss", null)
                            .setCancelable(false)
                            .show();
                }
            }
        );

        this.invalidateOptionsMenu();

        /**
         * When user selects the camera icon, check to see if we have permission to view their images.
         */
        add_observation_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_GALLERY);
                } else {
                    setGallery();
                }

                select.setVisibility(View.GONE);
                dialog_add_observation.setVisibility(View.VISIBLE);            }
        });

        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((NatureNetApplication)getApplication()).isConnected()) {
                    MainActivity.this.goToLoginActivity();
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((NatureNetApplication)getApplication()).isConnected()) {
                    MainActivity.this.goToJoinActivity();
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mFirebase = FirebaseDatabase.getInstance().getReference();
        showNoUser();

        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, new LaunchFragment())
                .commit();

        mUserAuthSubscription = ((NatureNetApplication)getApplication()).getCurrentUserObservable().subscribe(new Consumer<Optional<Users>>() {
            @Override
            public void accept(Optional<Users> user) throws Exception {
                if (user.isPresent()) {
                    if (signed_user == null) {
                        onUserSignIn(user.get());
                    }

                    if (getFragmentManager().getBackStackEntryCount() == 0) {
                        getFragmentManager()
                                .beginTransaction()
                                .add(R.id.fragment_container, ExploreFragment.newInstance(user_home_site))
                                .commitAllowingStateLoss();
                    }
                } else {
                    if (signed_user != null) {
                        onUserSignOut();
                    }
                    showNoUser();
                }
            }
        });

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(MainActivity.this, REQUEST_CODE_CHECK_LOCATION_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        Timber.w(e, "Unable to resolve location settings");
                    }
                } else if (status.getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                    requestLocationUpdates();
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                latValue = lastLocation.getLatitude();
                longValue = lastLocation.getLongitude();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_CHECK_LOCATION_SETTINGS);
        }

        latValue = 0.0;
        longValue = 0.0;

        dialog_add_observation = (LinearLayout) findViewById(R.id.ll_dialog_add_observation);
        add_observation_cancel = (ImageView) findViewById(R.id.dialog_add_observation_iv_cancel);
        camera = (Button) findViewById(R.id.dialog_add_observation_b_camera);
        gallery = (Button) findViewById(R.id.dialog_add_observation_b_gallery);
        select = (TextView) findViewById(R.id.dialog_add_observation_tv_select);
        gridview = (GridView) findViewById(R.id.dialog_add_observation_gv);
        gallery_item = (ImageView) findViewById(R.id.gallery_iv);
        cameraPhoto = new CameraPhoto(this);
        galleryPhoto = new GalleryPhoto(this);

        add_observation_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectedImages.clear();
                select.setVisibility(View.GONE);
                dialog_add_observation.setVisibility(View.GONE);
            }
        });

        /**
         * Click listener for when user selects images they want to upload.
         */
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Picasso.with(MainActivity.this).cancelTag(ImageGalleryAdapter.class.getSimpleName());
                observationPaths = selectedImages;
                setGallery();
                goToAddObservationActivity();
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGallery();
                select.setVisibility(View.GONE);

                try {
                    startActivityForResult(cameraPhoto.takePhotoIntent(), REQUEST_CODE_CAMERA);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Something Wrong while taking photo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGallery();
                select.setVisibility(View.GONE);

                //Check to see if the user is on API 18 or above.
                if(usingApiEighteenAndAbove()){
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent,"Select Picture"), GALLERY_IMAGES);
                }else{
                    //If not on 18 or above, go to the custom Gallery Activity
                    Intent intent = new Intent(getApplicationContext(), ImagePicker.class);
                    startActivityForResult(intent, IMAGE_PICKER_RESULTS);
                }


            }
        });

        dialog_add_observation.setVisibility(View.GONE);
    }

    /**
     * Sets the gallery of recent images when the user selects 'add observation' button.
     */
    public void setGallery() {
        Picasso.with(MainActivity.this).cancelTag(ImageGalleryAdapter.class.getSimpleName());
        recentImageGallery = getRecentImagesUris();

        if (recentImageGallery.size() != 0) {
            gridview.setAdapter(new ImageGalleryAdapter(this, recentImageGallery));

            //Here we handle clicks to the recent images. Let user select as many images as they want to submit.
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    ImageView iv = (ImageView) v.findViewById(R.id.gallery_iv);

                    //if the image the user selects hasn't been selected yet
                    if (!selectedImages.contains(recentImageGallery.get(position))) {
                        //add the clicked image to the selectedImages List
                        selectedImages.add(recentImageGallery.get(position));
                        iv.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.border_selected_image));
                        select.setVisibility(View.VISIBLE);
                    //here we handle the case of selecting an image that's already been selected
                    } else if (selectedImages.contains(recentImageGallery.get(position))) {
                        selectedImages.remove(recentImageGallery.get(position));
                        iv.setBackgroundResource(0);
                    }

                    //check to see if there are no selected images. if so, make select button 'unselectable'
                    if(selectedImages.size() == 0)
                        select.setVisibility(View.GONE);

                }
            });
        }
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
            case REQUEST_CODE_CHECK_LOCATION_SETTINGS:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        latValue = location.getLatitude();
        longValue = location.getLongitude();
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        Picasso.with(MainActivity.this).resumeTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_GALLERY);
        selectedImages.clear();
        select.setVisibility(View.GONE);
        if (mGoogleApiClient.isConnected()) {
            requestLocationUpdates();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Picasso.with(MainActivity.this).pauseTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_GALLERY);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Picasso.with(MainActivity.this).cancelTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_GALLERY);
        mUserAuthSubscription.dispose();
        super.onDestroy();
    }

    private void clearBackStack() {
        FragmentManager manager = getFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(0);
            manager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //if we have at least something in our selectionStack
        if(pastSelection!=0){
            //remove the highlight of the past selection
            navigationView.getMenu().findItem(pastSelection).setChecked(false);
            //if we have something left in our stack
            if(currentSelection!=0)
                //set it as the currently highlighted item
                navigationView.getMenu().findItem(currentSelection).setChecked(true);
        }

        return true;
    }

    /**
    *   Override back button action.
     */
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(getFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        } else if(getFragmentManager().getBackStackEntryCount() > 0){
            //we must redraw the menu
            this.invalidateOptionsMenu();
            //store id of menu item that we will be un-highlighting
            pastSelection = selectionStack.pop();
            //if we still have items in our stack
            if(selectionStack.size()>0){
                //store the current selection
                currentSelection = selectionStack.peek();
            }else
                currentSelection = 0;   //otherwise, set the current selection as 0 so we know we've reached the end of our stack
            super.onBackPressed();
        }else
            super.onBackPressed();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.nav_explore:
                selectionStack.add(R.id.nav_explore);
                goToExploreFragment();
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_projects:
                selectionStack.add(R.id.nav_projects);
                goToProjectsFragment();
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_design_ideas:
                selectionStack.add(R.id.nav_design_ideas);
                goToDesignIdeasFragment();
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_communities:
                selectionStack.add(R.id.nav_communities);
                goToCommunitiesFragment();
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_logout:
                //set current selection as 0 so we know there isn't anything selected from the menu
                currentSelection=0;
                FirebaseAuth.getInstance().signOut();
                break;
        }
        return true;
    }

    public void showLaunchFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new LaunchFragment())
                .commit();
    }

    public void goToExploreFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ExploreFragment.newInstance(user_home_site))
                .addToBackStack(ExploreFragment.FRAGMENT_TAG)
                .commit();
    }

    public void goToProjectsFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProjectsFragment())
                .addToBackStack(ProjectsFragment.FRAGMENT_TAG)
                .commit();
    }

    public void goToDesignIdeasFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new IdeasFragment())
                .addToBackStack(IdeasFragment.FRAGMENT_TAG)
                .commit();
    }

    public void goToCommunitiesFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CommunitiesFragment())
                .addToBackStack(CommunitiesFragment.FRAGMENT_TAG)
                .commit();
    }

    public void goToJoinActivity() {
        ids = new ArrayList<>();
        names = new ArrayList<>();
        mFirebase.child(Site.NODE_NAME).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Site site = postSnapshot.getValue(Site.class);
                    ids.add(site.id);
                    names.add(site.name);
                }
                if (ids.size() != 0 && names.size() != 0) {
                    affiliation_ids = ids.toArray(new String[ids.size()]);
                    affiliation_names = names.toArray(new String[names.size()]);
                    Intent join = new Intent(getApplicationContext(), JoinActivity.class);
                    join.putExtra(JoinActivity.EXTRA_SITE_IDS, affiliation_ids);
                    join.putExtra(JoinActivity.EXTRA_SITE_NAMES, affiliation_names);
                    startActivityForResult(join, REQUEST_CODE_JOIN);
                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.join_error_message_firebase_read) + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void goToLoginActivity() {
        Intent login = new Intent(this, LoginActivity.class);
        startActivityForResult(login, REQUEST_CODE_LOGIN);
    }

    public void goToAddObservationActivity() {
        Intent addObservation = new Intent(this, AddObservationActivity.class);
        addObservation.putParcelableArrayListExtra(AddObservationActivity.EXTRA_IMAGE_PATH, selectedImages);
        addObservation.putExtra(AddObservationActivity.EXTRA_LATITUDE, latValue);
        addObservation.putExtra(AddObservationActivity.EXTRA_LONGITUDE, longValue);
        addObservation.putExtra(AddObservationActivity.EXTRA_USER, signed_user);
        startActivity(addObservation);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    public void goToAddDesignIdeaActivity(){
        Intent addDesignIdeaIntent = new Intent(this, AddDesignIdeaActivity.class);
        startActivity(addDesignIdeaIntent);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    public void goToProjectActivity(Project p) {
        Intent project = new Intent(this, ProjectActivity.class);
        project.putExtra(ProjectActivity.EXTRA_PROJECT, p);
        startActivity(project);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    public void goToObservationActivity() {
        Intent observation = new Intent(this, ObservationActivity.class);
        if (previewSelectedObservation != null) {
            observation.putExtra(ObservationActivity.EXTRA_OBSERVATION_ID, previewSelectedObservation.id);
        }
        startActivity(observation);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    /**
     * This method gets all the recent images the user has taken.
     * @return listOfAllImages - the list of all the most recent images taken on the phone.
     */
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (REQUEST_CODE_JOIN): {
                if (resultCode == Activity.RESULT_OK) {
                    if (JoinActivity.EXTRA_LAUNCH.equals(data.getExtras().getString(JoinActivity.EXTRA_JOIN))) {
                        showLaunchFragment();
                    } else if (JoinActivity.EXTRA_LOGIN.equals(data.getExtras().getString(JoinActivity.EXTRA_JOIN))) {
                        signed_user = data.getParcelableExtra(JoinActivity.EXTRA_NEW_USER);
                        logout.setVisible(true);
                        this.supportInvalidateOptionsMenu();

                        if (signed_user.avatar != null) {
                            NatureNetUtils.showUserAvatar(this, nav_iv, signed_user.avatar);
                        }

                        display_name.setText(signed_user.displayName);
                        mFirebase.child(Site.NODE_NAME).child(signed_user.affiliation).child(NAME).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                affiliation.setText((String)snapshot.getValue());
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Timber.w("Could not get user's affiliation");
                            }
                        });
                        sign_in.setVisibility(View.GONE);
                        join.setVisibility(View.GONE);
                        display_name.setVisibility(View.VISIBLE);
                        affiliation.setVisibility(View.VISIBLE);
                        goToExploreFragment();
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
                break;
            }
            case(REQUEST_CODE_LOGIN): {
                if (resultCode == Activity.RESULT_OK) {
                    if (data.getStringExtra(LoginActivity.EXTRA_LOGIN).equals(LoginActivity.EXTRA_JOIN)) {
                        goToJoinActivity();
                    } else {
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
                break;
            }
            case REQUEST_CODE_CAMERA: {
                if (resultCode == MainActivity.RESULT_OK) {
                    Timber.d("Camera Path: %s", cameraPhoto.getPhotoPath());
                    observationPath = Uri.fromFile(new File(cameraPhoto.getPhotoPath()));
                    cameraPhoto.addToGallery();
                    setGallery();
                    goToAddObservationActivity();
                }
                break;
            }
            case REQUEST_CODE_GALLERY: {
                if (resultCode == MainActivity.RESULT_OK) {
                    galleryPhoto.setPhotoUri(data.getData());
                    Timber.d("Gallery Path: %s", galleryPhoto.getPath());
                    observationPath = Uri.fromFile(new File(galleryPhoto.getPath()));
                    setGallery();
                    goToAddObservationActivity();
                }
                break;
            }
            //This case is for retrieving images from the phone's Gallery app.
            case GALLERY_IMAGES: {
                //First, make sure the the user actually chose something.
                if(data != null){
                    //In this case, the user selected multiple images
                    if(data.getClipData() != null){

                        for(int j = 0; j<data.getClipData().getItemCount(); j++){
                            selectedImages.add(data.getClipData().getItemAt(j).getUri());
                            Log.d("images", "selected image: " + data.getClipData().getItemAt(j).toString());
                        }
                    }
                    //in this case, the user selected just one image
                    else if(data.getData() != null){
                        selectedImages.add(data.getData());
                    }

                    //Here we should have our selected images
                    goToAddObservationActivity();
                }
                break;
            }
            //This case is for retrieving images from the custom Gallery (phones using api <18).
            case IMAGE_PICKER_RESULTS: {
                if(resultCode == MainActivity.RESULT_OK){
                    selectedImages = data.getParcelableArrayListExtra("images");
                    goToAddObservationActivity();
                }
            }
        }
    }
    public void onUserSignIn(@NonNull Users user) {
        signed_user = user;
        mFirebase.child(Site.NODE_NAME).child(signed_user.affiliation).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_home_site = dataSnapshot.getValue(Site.class);
                if(user_home_site != null && !mGoogleApiClient.isConnected()) {
                    latValue = user_home_site.location.get(0);
                    longValue = user_home_site.location.get(1);
                }
                showUserInfo(signed_user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, getString(R.string.login_error_message_firebase_read), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onUserSignOut() {
        if (signed_user != null) {
            Toast.makeText(this, "You have been logged out.", Toast.LENGTH_SHORT).show();
            mFirebase.child(Users.NODE_NAME).child(signed_user.id).keepSynced(false);
            signed_user = null;
        }
        user_home_site = null;
        this.invalidateOptionsMenu();
        clearBackStack();
        showLaunchFragment();
    }

    public void showNoUser() {
        NatureNetUtils.showUserAvatar(this, nav_iv, R.drawable.default_avatar);
        logout.setVisible(false);
        display_name.setText(null);
        affiliation.setText(null);
        display_name.setVisibility(View.GONE);
        affiliation.setVisibility(View.GONE);
        sign_in.setVisibility(View.VISIBLE);
        join.setVisibility(View.VISIBLE);
    }

    public void showUserInfo(final Users user) {
        NatureNetUtils.showUserAvatar(this, nav_iv, user.avatar);
        logout.setVisible(true);
        display_name.setText(user.displayName);
        affiliation.setText(user_home_site.name);
        display_name.setVisibility(View.VISIBLE);
        affiliation.setVisibility(View.VISIBLE);
        sign_in.setVisibility(View.GONE);
        join.setVisibility(View.GONE);
        drawer.openDrawer(GravityCompat.START);
    }

    public boolean usingApiEighteenAndAbove(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
}