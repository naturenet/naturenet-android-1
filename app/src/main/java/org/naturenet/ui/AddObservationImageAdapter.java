package org.naturenet.ui;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.naturenet.R;

import java.util.ArrayList;


public class AddObservationImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Uri> images;

    public AddObservationImageAdapter(Context mContext, ArrayList<Uri> images) {
        this.mContext = mContext;
        this.images = images;
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

        Picasso.with(mContext).load(images.get(i)).into(iv);

        return view;

    }
}
