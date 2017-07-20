package org.naturenet.util;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.naturenet.R;
import org.naturenet.data.model.Site;
import org.naturenet.data.model.TimestampedData;
import org.naturenet.data.model.Users;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class NatureNetUtils {

    public enum PICASSO_TAGS {
        PICASSO_TAG_PROJECT_LIST,
        PICASSO_TAG_OBSERVATION_LIST,
        PICASSO_TAG_GALLERY,
    }

    private static final Transformation mAvatarTransform = new CroppedCircleTransformation();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    public static String toDateString(TimestampedData data) {
        Long timestamp = data.getUpdatedAtMillis();
        if (timestamp == null) {
            return null;
        }
        return dateFormat.format(new Date(data.getUpdatedAtMillis()));
    }

    public static void showUserAvatar(final Context context, final ImageView view, final String avatarUrl) {
        Picasso p = Picasso.with(context);
        p.setIndicatorsEnabled(false);
        p.load(Strings.emptyToNull(avatarUrl)).transform(mAvatarTransform)
                .placeholder(R.drawable.default_avatar).error(R.drawable.default_avatar).noFade().fit().into(view);
    }

    public static void showUserAvatar(final Context context, final ImageView view, final int avatarId) {
        Picasso.with(context).load(avatarId).transform(mAvatarTransform)
                .placeholder(R.drawable.default_avatar).error(R.drawable.default_avatar).noFade().fit().into(view);
    }

    public static void makeUserBadge(final Context context, final ViewGroup root, final String userId) {

        View badge = View.inflate(context, R.layout.badge_horizontal, root);
        final ImageView avatar = (ImageView) badge.findViewById(R.id.badge_image);
        final TextView name = (TextView) badge.findViewById(R.id.badge_title);
        final TextView affiliation = (TextView) badge.findViewById(R.id.badge_subtitle);

        FirebaseDatabase.getInstance().getReference(Users.NODE_NAME).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Users user = dataSnapshot.getValue(Users.class);
                name.setText(user.displayName);
                Picasso.with(context)
                        .load(Strings.emptyToNull(user.avatar))
                        .transform(mAvatarTransform)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .noFade()
                        .fit()
                        .priority(Picasso.Priority.LOW)
                        .tag(PICASSO_TAGS.PICASSO_TAG_OBSERVATION_LIST)
                        .into(avatar);

                FirebaseDatabase.getInstance().getReference(Site.NODE_NAME).child(user.affiliation)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Site site = dataSnapshot.getValue(Site.class);
                        affiliation.setText(site.name);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.w(databaseError.toException(), "Unable to read data for user %s", userId);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.w(databaseError.toException(), "Unable to read data for user %s", userId);
            }
        });
    }
}
