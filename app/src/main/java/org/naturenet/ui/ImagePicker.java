package org.naturenet.ui;

import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.naturenet.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * This class serves as a "Gallery" for phones on API < 18. Those above API 18 will use the phone's default Gallery for image choosing.
 */
public class ImagePicker extends AppCompatActivity {

    private TextView select, loadMoreButton;
    private GridView imageGrid;
    private List<Uri> images, loadedImages;
    private ArrayList<Uri> selectedImages;
    private Cursor cursor;
    private int numImagesToLoad;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);

        select = (TextView) findViewById(R.id.selectButton);
        loadMoreButton = (TextView) findViewById(R.id.loadMoreButton);
        select.setVisibility(View.GONE);
        imageGrid = (GridView) findViewById(R.id.imageGridView);
        images = new ArrayList<>();
        loadedImages  = new ArrayList<>();
        selectedImages = new ArrayList<>();
        numImagesToLoad = 20;

        //prepare cursor to read images stored on phone
        prepCursor();

        if(cursor!=null){
            loadedImages = getUris(numImagesToLoad);
            final ImageGalleryAdapter adapter = new ImageGalleryAdapter(this, loadedImages);
            imageGrid.setAdapter(adapter);

            imageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    ImageView iv = (ImageView) view.findViewById(R.id.gallery_iv);

                    //if the image the user selects hasn't been selected yet
                    if (!selectedImages.contains(loadedImages.get(i))) {
                        //add the clicked image to the selectedImages List
                        selectedImages.add(loadedImages.get(i));
                        iv.setBackground(ContextCompat.getDrawable(ImagePicker.this, R.drawable.border_selected_image));
                        adapter.addSelectedImage(i);
                        select.setVisibility(View.VISIBLE);
                        //here we handle the case of selecting an image that's already been selected
                    } else if (selectedImages.contains(loadedImages.get(i))) {
                        selectedImages.remove(loadedImages.get(i));
                        iv.setBackgroundResource(0);
                        adapter.removeSelectedImage(i);
                    }

                    //check to see if there are no selected images. if so, make select button 'unselectable'
                    if(selectedImages.size() == 0)
                        select.setVisibility(View.GONE);
                }
            });

            /*
            Pressing the load more button increments the number of images to load by 16. We then pass this to the getUris() method.
            We then update the adapter and scroll the GridView to the bottom.
         */
            loadMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    numImagesToLoad += 16;
                    loadedImages = getUris(numImagesToLoad);
                    adapter.notifyDataSetChanged();
                    imageGrid.smoothScrollToPosition(numImagesToLoad-1);
                }
            });
        }

        /*
            When the select button is pressed, a new Intent is created and set as the result of the Intent that brought the user here.
            The contents of the intent are the selected image Uris.
         */
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                resultIntent.putParcelableArrayListExtra("images", selectedImages);
                setResult(MainActivity.RESULT_OK, resultIntent);
                finish();
            }
        });

    }

    /**
     * This method prepares a Crusor to read images that are on the user's phone.
     */
    private void prepCursor(){
        Uri uri;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[] { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN };
        cursor = this.getContentResolver().query(uri, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if(cursor != null) {
            cursor.moveToFirst();
        }else
            Toast.makeText(this, "Could not load images!", Toast.LENGTH_SHORT).show();
    }

    /**
     * This method serves the purpose of retrieving image Uris for the user to select.
     * @param numImages - the number of images to have displayed at one time.
     * @return List of image Uris
     */
    private List<Uri> getUris(int numImages) {

        if (cursor != null) {
            try {
                do {
                    images.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                            new File(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))));
                } while (cursor.moveToNext() && images.size() < numImages);
            } catch (CursorIndexOutOfBoundsException ex) {
                Timber.e(ex, "Could not read data from MediaStore, image gallery may be empty");
            }
        }else {
            Timber.e("Could not get MediaStore content!");
        }
        return images;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cursor.close();
    }
}
