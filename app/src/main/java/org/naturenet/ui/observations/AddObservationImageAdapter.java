package org.naturenet.ui.observations;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.naturenet.R;
import org.naturenet.util.NatureNetUtils;

import java.util.ArrayList;


public class AddObservationImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Uri> images;
    private boolean fromCamera;

    public AddObservationImageAdapter(Context mContext, ArrayList<Uri> images, boolean fromCamera) {
        this.mContext = mContext;
        this.images = images;
        this.fromCamera = fromCamera;
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int i) {
        return images.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.observation_image, viewGroup, false);
        }

        ImageView iv = (ImageView) view.findViewById(R.id.image_for_flipper);

        if(fromCamera)
            NatureNetUtils.showImage(mContext, iv, images.get(i), false, fromCamera);
        else
            NatureNetUtils.showImage(mContext, iv, images.get(i), false, fromCamera);

        return view;

    }
}
