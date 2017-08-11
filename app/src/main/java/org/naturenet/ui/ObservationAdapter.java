package org.naturenet.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.common.base.Strings;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import org.naturenet.R;
import org.naturenet.data.model.Observation;
import org.naturenet.util.NatureNetUtils;

public class ObservationAdapter extends FirebaseListAdapter<Observation> {

    Picasso picasso;

    ObservationAdapter(Activity activity, Query query) {
        super(activity, Observation.class, R.layout.observation_list_item, query);
        picasso = Picasso.with(activity);
        picasso.setIndicatorsEnabled(false);
    }

    @Override
    protected void populateView(final View v, final Observation model, int position) {
        v.setTag(model);
        ViewGroup badge = (ViewGroup) v.findViewById(R.id.observation_user_badge);
        badge.removeAllViews();
        NatureNetUtils.makeUserBadge(mActivity, badge, model.userId);
        picasso.load(Strings.emptyToNull(model.data.image))
                .placeholder(R.drawable.default_image)
                .error(R.drawable.no_image)
                .fit()
                .centerCrop()
                .tag(NatureNetUtils.PICASSO_TAGS.PICASSO_TAG_OBSERVATION_LIST)
                .into((ImageView) v.findViewById(R.id.observation_icon));
    }

    @Override
    public Observation getItem(int pos) {
        return super.getItem(getCount() - 1 - pos);
    }
}