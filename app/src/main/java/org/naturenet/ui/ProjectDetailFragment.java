package org.naturenet.ui;

import android.Manifest;
import android.app.Fragment;
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
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.kosalgeek.android.photoutil.CameraPhoto;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Observation;
import org.naturenet.data.model.Project;
import org.naturenet.util.NatureNetUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ProjectDetailFragment extends Fragment {

    static String COMPLETED = "Completed";
    static String FRAGMENT_TAG = "project_detail_fragment";
    private static final String ARG_PROJECT = "ARG_PROJECT";

    static final private int REQUEST_CODE_CAMERA = 3;
    static final private int REQUEST_CODE_GALLERY = 4;
    static final private int IMAGE_PICKER_RESULTS = 6;
    static final private int GALLERY_IMAGES = 100;

    private TextView mName, mStatus, mDescription, mEmpty, select;
    private ImageView mIcon, mStatusIcon;
    private GridView mGvObservations, gridview;
    private Project mProject;
    private ImageView addObs, add_observation_cancel;
    private LinearLayout dialog_add_obs;
    private Button camera, gallery;
    private List<Uri> recentImageGallery;
    private ArrayList<Uri> selectedImages;
    private CameraPhoto cameraPhoto;
    private boolean imagesSelected = false;

    public static ProjectDetailFragment newInstance(Project p) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROJECT, p);
        ProjectDetailFragment frag = new ProjectDetailFragment();
        frag.setArguments(args);
        frag.setRetainInstance(true);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mName = (TextView) view.findViewById(R.id.project_tv_name);
        mStatus = (TextView) view.findViewById(R.id.project_tv_status);
        mDescription = (TextView) view.findViewById(R.id.project_tv_description);
        mEmpty = (TextView) view.findViewById(R.id.project_tv_no_recent_contributions);
        mIcon = (ImageView) view.findViewById(R.id.project_iv_icon);
        mStatusIcon = (ImageView) view.findViewById(R.id.project_iv_status);
        mGvObservations = (GridView) view.findViewById(R.id.observation_gallery);
        addObs = (ImageView) getActivity().getWindow().findViewById(R.id.addObsButton);
        dialog_add_obs = (LinearLayout) getActivity().getWindow().findViewById(R.id.ll_dialog_add_observation);
        add_observation_cancel = (ImageView) getActivity().getWindow().findViewById(R.id.dialog_add_observation_iv_cancel);
        camera = (Button) getActivity().getWindow().findViewById(R.id.dialog_add_observation_b_camera);
        gallery = (Button) getActivity().getWindow().findViewById(R.id.dialog_add_observation_b_gallery);
        select = (TextView) getActivity().getWindow().findViewById(R.id.dialog_add_observation_tv_select);
        gridview = (GridView) getActivity().getWindow().findViewById(R.id.dialog_add_observation_gv);
        selectedImages = new ArrayList<>();
        cameraPhoto = new CameraPhoto(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() == null || getArguments().getParcelable(ARG_PROJECT) == null) {
            Timber.e(new IllegalArgumentException(), "Tried to load ProjectDetailFragment without a Project argument");
            Toast.makeText(getActivity(), "No project to display", Toast.LENGTH_SHORT).show();
            return;
        }
        mProject = getArguments().getParcelable(ARG_PROJECT);

        mName.setText(mProject.name);
        dialog_add_obs.setVisibility(View.GONE);
        select.setVisibility(View.GONE);

        if (mProject.status != null) {
            mStatus.setText(mProject.status);

            if (mProject.status.equals(COMPLETED)) {
                mStatusIcon.setVisibility(View.VISIBLE);
            } else {
                mStatusIcon.setVisibility(View.GONE);
            }
        } else {
            mStatusIcon.setVisibility(View.GONE);
        }

        if (mProject.description != null) {
            mDescription.setText(mProject.description);
            mDescription.setMovementMethod(ScrollingMovementMethod.getInstance());
        }

        if (mProject.iconUrl != null) {
            Picasso.with(getActivity())
                    .load(Strings.emptyToNull(mProject.iconUrl))
                    .fit()
                    .into(mIcon);
        }

        Query query = FirebaseDatabase.getInstance().getReference(Observation.NODE_NAME)
                .orderByChild("activity").equalTo(mProject.id).limitToLast(20);
        ObservationAdapter adapter = new ObservationAdapter(getActivity(), query);
        mGvObservations.setAdapter(adapter);
        mGvObservations.setEmptyView(mEmpty);

        mGvObservations.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                if (scrollState == SCROLL_STATE_FLING) {
                    Picasso.with(getActivity()).pauseTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_OBSERVATION_LIST);
                } else {
                    Picasso.with(getActivity()).resumeTag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_OBSERVATION_LIST);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {}
        });

        mGvObservations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent observationIntent = new Intent(getActivity(), ObservationActivity.class);
                observationIntent.putExtra(ObservationActivity.EXTRA_OBSERVATION_ID, ((Observation)view.getTag()).id);
                startActivity(observationIntent);
            }
        });

        addObs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_GALLERY);
                } else {
                    setGallery();
                }

                select.setVisibility(View.GONE);
                dialog_add_obs.setVisibility(View.VISIBLE);
            }
        });

        add_observation_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_add_obs.setVisibility(View.GONE);
                select.setVisibility(View.GONE);
                selectedImages.clear();
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isStoragePermitted()){

                    setGallery();
                    select.setVisibility(View.GONE);
                    selectedImages.clear();

                    //Check to see if the user is on API 18 or above.
                    if(usingApiEighteenAndAbove()){
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent,"Select Picture"), GALLERY_IMAGES);
                    }else{
                        //If not on 18 or above, go to the custom Gallery Activity
                        Intent intent = new Intent(getActivity(), ImagePicker.class);
                        startActivityForResult(intent, IMAGE_PICKER_RESULTS);
                    }
                }else
                    Toast.makeText(getActivity(), R.string.permission_rejected, Toast.LENGTH_LONG).show();


            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //make sure storage is permitted
                if(isStoragePermitted()){
                    setGallery();
                    select.setVisibility(View.GONE);

                    try {
                        startActivityForResult(cameraPhoto.takePhotoIntent(), REQUEST_CODE_CAMERA);
                    } catch (IOException e) {
                        Toast.makeText(getActivity(), "Something Wrong while taking photo", Toast.LENGTH_SHORT).show();
                    }
                }else
                    Toast.makeText(getActivity(), R.string.permission_rejected, Toast.LENGTH_LONG).show();

            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToAddObservationActivity(mProject);
            }
        });
    }

    private void setGallery(){
        Picasso.with(getActivity()).cancelTag(ImageGalleryAdapter.class.getSimpleName());
        recentImageGallery = getRecentImagesUris();

        if (recentImageGallery.size() != 0) {
            gridview.setAdapter(new ImageGalleryAdapter(getActivity(), recentImageGallery));

            //Here we handle clicks to the recent images. Let user select as many images as they want to submit.
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    ImageView iv = (ImageView) v.findViewById(R.id.gallery_iv);

                    //if the image the user selects hasn't been selected yet
                    if (!selectedImages.contains(recentImageGallery.get(position))) {
                        //add the clicked image to the selectedImages List
                        selectedImages.add(recentImageGallery.get(position));
                        iv.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.border_selected_image));
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
        cursor = getActivity().getContentResolver().query(uri, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                do {
                    listOfAllImages.add(FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider",
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_CAMERA:
                if (resultCode == MainActivity.RESULT_OK) {
                    Timber.d("Camera Path: %s", cameraPhoto.getPhotoPath());
                    selectedImages.add(Uri.fromFile(new File(cameraPhoto.getPhotoPath())));
                    cameraPhoto.addToGallery();
                    setGallery();
                    imagesSelected = true;
                    goToAddObservationActivity(mProject);
                }
                break;
            case GALLERY_IMAGES:
                //First, make sure the the user actually chose something.
                if(data != null){
                    //In this case, the user selected multiple images
                    if(data.getClipData() != null){

                        for(int j = 0; j<data.getClipData().getItemCount(); j++){
                            selectedImages.add(data.getClipData().getItemAt(j).getUri());
                        }
                    }
                    //in this case, the user selected just one image
                    else if(data.getData() != null){
                        selectedImages.add(data.getData());
                    }

                    //Here we should have our selected images
                    imagesSelected = true;
                    goToAddObservationActivity(mProject);

                }
                break;
            case IMAGE_PICKER_RESULTS:
                if(resultCode == MainActivity.RESULT_OK){
                    selectedImages = data.getParcelableArrayListExtra("images");
                    goToAddObservationActivity(mProject);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_GALLERY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    setGallery();
                else
                    Toast.makeText(getActivity(), "Gallery Access Permission Denied", Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    setGallery();
                else
                    Toast.makeText(getActivity(), "Camera Access Permission Denied", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    private void goToAddObservationActivity(Project p){
        Intent addObservation = new Intent(getActivity(), AddObservationActivity.class);
        addObservation.putParcelableArrayListExtra(AddObservationActivity.EXTRA_IMAGE_PATH, selectedImages);
        addObservation.putExtra(AddObservationActivity.EXTRA_LATITUDE, MainActivity.latValue);
        addObservation.putExtra(AddObservationActivity.EXTRA_LONGITUDE, MainActivity.longValue);
        addObservation.putExtra(AddObservationActivity.EXTRA_USER, MainActivity.signed_user);
        addObservation.putExtra(AddObservationActivity.EXTRA_PROJECT, p);
        startActivity(addObservation);
    }

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

    public boolean usingApiEighteenAndAbove(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    @Override
    public void onResume() {
        if(isStoragePermitted())
            setGallery();
        if(!imagesSelected)
            selectedImages.clear();

        select.setVisibility(View.GONE);
        imagesSelected = false;
        super.onResume();

    }
}