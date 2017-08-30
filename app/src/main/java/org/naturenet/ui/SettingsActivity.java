package org.naturenet.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.naturenet.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.naturenet.BuildConfig.SERVER_KEY;

public class SettingsActivity extends AppCompatActivity implements Settings{

    private static String url = "https://iid.googleapis.com/iid/info/<TOKEN>?details=true";

    Switch newProjectSwitch, newIdeaSwitch;
    ProgressBar progressBar;
    String notification_token;
    CompoundButton.OnCheckedChangeListener checkedChangeListenerProject, checkedChangeListenerIdea;
    Toolbar toolbar;
    RelativeLayout settingsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        //Get the user's notification token
        notification_token = getIntent().getStringExtra("token");
        newProjectSwitch = (Switch) findViewById(R.id.new_project_switch);
        newIdeaSwitch = (Switch) findViewById(R.id.new_idea_switch);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        settingsLayout = (RelativeLayout) findViewById(R.id.settingsLayout);

        checkedChangeListenerProject = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //If the user checks the Projects button, subscribe to that topic. Otherwise, unsubscribe from it
                if(b)
                    FirebaseMessaging.getInstance().subscribeToTopic("activities");
                else
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("activities");
            }
        };

        checkedChangeListenerIdea = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //If the user checks the Ideas button, subscribe to that topic. Otherwise, unsubscribe from it
                if(b)
                    FirebaseMessaging.getInstance().subscribeToTopic("ideas");
                else
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("ideas");
            }
        };

        //Put the user's notification token in our http request
        String newUrl = url.replace("<TOKEN>", notification_token);
        //retrieve the user's notification preferences
        new RetrieveTopics(SettingsActivity.this).execute(newUrl);
    }

    /**
     * Simply make the progress bar visible to reflect to the user that we are retrieving their settings.
     */
    @Override
    public void setProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * This method is called when the http request is done. Here we cancel the progress bar, and set the settings to their proper values.
     * @param activities - Boolean value reflecting whether or not the user is subscribed to Projects notifications.
     * @param ideas - Boolean value reflecting whether or not the user is subscribed to Ideas notifications.
     */
    @Override
    public void cancelProgress(boolean activities, boolean ideas) {
        progressBar.setVisibility(View.GONE);

        newProjectSwitch.setChecked(activities);
        newIdeaSwitch.setChecked(ideas);

        //Set listeners for whenever the user toggles the notifications they want to receive.
        newProjectSwitch.setOnCheckedChangeListener(checkedChangeListenerProject);
        newIdeaSwitch.setOnCheckedChangeListener(checkedChangeListenerIdea);
    }

    @Override
    public void disableWindow() {
        settingsLayout.setVisibility(View.GONE);
    }

    @Override
    public void enableWindow() {
        settingsLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //handles back button presses on toolbar
                Intent resultIntent = new Intent(this, MainActivity.class);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                overridePendingTransition(R.anim.stay, R.anim.slide_down);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent resultIntent = new Intent(this, MainActivity.class);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down);
    }
}

/**
 * AsyncTask responsible for querying for the user's notification preferences (Projects, Ideas).
 */
class RetrieveTopics extends AsyncTask<String, Void, String> {

    Settings activity;

    public RetrieveTopics(Settings fragment){
        this.activity = fragment;
    }

    /**
     * Set the progress bar.
     */
    @Override
    protected void onPreExecute() {
        activity.setProgress();
        activity.disableWindow();
    }

    /**
     * DoInBackground method responsible for the meat of the http request.
     * @param strings - String array containing the url.
     * @return
     */
    @Override
    protected String doInBackground(String... strings) {

        StringBuilder stringBuilder = new StringBuilder();
        try {
            //Create url object and open url connection
            URL url = new URL(strings[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //Set some headers
            connection.setRequestProperty("Authorization", "key=" + SERVER_KEY);
            connection.setRequestProperty("Content-Type", "application/json");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            //Read the response
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    /**
     * Method triggered when the request has been completed.
     * @param response - The response from the http request.
     */
    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        boolean activities = false, ideas = false;
        JSONObject rel, topics;

        //Parse the JSON response.
        try {
            JSONObject jsonObject = (JSONObject) new JSONTokener(response).nextValue();

            if(jsonObject.has("rel")){
                rel = jsonObject.getJSONObject("rel");
                if(rel.has("topics")){
                    topics = rel.getJSONObject("topics");

                    if(topics.has("activities"))
                        activities = true;
                    if(topics.has("ideas"))
                        ideas = true;

                }
            }

            //Set the corresponding values for the user's setting preferences.
            activity.cancelProgress(activities, ideas);
            activity.enableWindow();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}

interface Settings{
    void setProgress();
    void cancelProgress(boolean activities, boolean ideas);
    void disableWindow();
    void enableWindow();
}
