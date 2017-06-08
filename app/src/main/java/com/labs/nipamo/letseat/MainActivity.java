package com.labs.nipamo.letseat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import static com.labs.nipamo.letseat.FindPlacesConfig.APIKEY;
import static com.labs.nipamo.letseat.FindPlacesConfig.NAME;
import static com.labs.nipamo.letseat.FindPlacesConfig.OK;
import static com.labs.nipamo.letseat.FindPlacesConfig.PRICE;
import static com.labs.nipamo.letseat.FindPlacesConfig.RATING;
import static com.labs.nipamo.letseat.FindPlacesConfig.RESULTS;
import static com.labs.nipamo.letseat.FindPlacesConfig.STATUS;
import static com.labs.nipamo.letseat.FindPlacesConfig.VICINITY;
import static com.labs.nipamo.letseat.FindPlacesConfig.ZERO_RESULTS;
import static com.labs.nipamo.letseat.SettingsActivity.PREFERENCES;

public class MainActivity extends AppCompatActivity {

    private String url;
    private int prev;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the app bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        // Set up the ad banner
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);
        */
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /* Called when the user taps a button in the menu */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        switch (item.getItemId()){
            case R.id.options:
                intent = new Intent(this, OptionsActivity.class);
                startActivity(intent);
                return true;

            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Called when the user taps the "Show Map" button */
    public void viewMap(View view){
        // Restore the saved data
        sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        // Check if the user selected a location setting
       if (sharedPreferences != null){
            // Start the Maps Activity
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        } else{
           // Go to the settings activity so the user can enter location setting
           Intent intent = new Intent (this, SettingsActivity.class);
           startActivity(intent);
           Toast toast = Toast.makeText(MainActivity.this,
                   "Error: Location settings are not configured", Toast.LENGTH_LONG);
           toast.show();
       }
    }

    /* Called when the user taps the "View List" button */
    public void viewList(View view){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    /* Called when the user taps the "Details" button */
    public void showDetails(View view){
        Intent intent = new Intent (this, DetailsActivity.class);
        startActivity(intent);
    }

    /* Called when the user taps the "Let's Eat" button */
    public void letsEat(View view){
        double latitude;
        double longitude;

        // Restore the saved data
        sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        // Check if the user selected a location setting
        if (sharedPreferences != null){
            // Use FindLocation to get the latitude and longitude
            ((FindLocation) getApplicationContext()).setLocation();
            latitude = ((FindLocation) getApplicationContext()).getLatitude();
            longitude = ((FindLocation) getApplicationContext()).getLongitude();

            // Create the url and execute the async task
            loadNearbyPlaces(latitude, longitude);
            JSONTaskMain json = new JSONTaskMain();
            json.execute();
        } else{
            // Go to the settings activity so the user can enter location setting
            Intent intent = new Intent (this, SettingsActivity.class);
            startActivity(intent);
            Toast toast = Toast.makeText(MainActivity.this,
                    "Error: Location settings are not configured", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /* Creates a url to be passed to the async task */
    private void loadNearbyPlaces(double latitude, double longitude){
        // Set up local variables
        String type = "restaurant";
        int distance = ((FindPlacesConfig) getApplicationContext()).getDistance();
        String category = ((FindPlacesConfig) getApplicationContext()).getCategory();

        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&radius=").append(distance);
        googlePlacesUrl.append("&types=").append(type);
        if (category != null){
            googlePlacesUrl.append("&keyword=").append(category);
        }
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + APIKEY);

        this.url = googlePlacesUrl.toString();
    }

    /* Async task for getting the JSON result */
    private class JSONTaskMain extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String googlePlacesUrl = MainActivity.this.url;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                    googlePlacesUrl, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject result) {
                            parseLocationResult(result);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getBaseContext(), "Error: No response",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
            FindPlaces.getInstance().addToRequestQueue(request);
            return "Executed";
        }

        @Override
        protected void onPostExecute(String status) {
            // Set up the local variables
            TextView result = (TextView) findViewById(R.id.result);
            Button details = (Button) findViewById(R.id.details_button);

            // Display the result
            String name = ((FindPlaces) getApplicationContext()).getName();
            if (name != null){
                result.setText(name);
                result.setVisibility(View.VISIBLE);
                details.setVisibility(View.VISIBLE);
            }else{
                Toast toast = Toast.makeText(MainActivity.this,
                        "Error: Name is null", Toast.LENGTH_LONG);
                toast.show();
            }
        }

        /* Get the place name, location, rating and price */
        private void parseLocationResult(JSONObject result) {
            // Set up local variables
            String placeName, placeLocation, placeRating, placePrice;
            Random rand = new Random();

            try {
                JSONArray jsonArray = result.getJSONArray(RESULTS);

                if (result.getString(STATUS).equalsIgnoreCase(OK)) {
                    // Pick a random place
                    int i = MainActivity.this.prev;
                    if (jsonArray.length() == 1){
                        // Only one result
                        Toast.makeText(getBaseContext(), "Not other restaurant found",
                                Toast.LENGTH_LONG).show();
                    }else {
                        while (i == MainActivity.this.prev) {
                            i = rand.nextInt(jsonArray.length());
                        }
                    }
                    MainActivity.this.prev = i;
                    JSONObject place = jsonArray.getJSONObject(i);
                    if (!place.isNull(NAME)) {
                        placeName = place.getString(NAME);
                        if (place.has(VICINITY)) {
                            placeLocation = place.getString(VICINITY);
                        }else{
                            placeLocation = "?";
                        }
                        if (place.has(RATING)){
                            placeRating = place.getString(RATING);
                        }else{
                            placeRating = "?";
                        }
                        if (place.has(PRICE)){
                            placePrice = place.getString(PRICE);

                        }else{
                            placePrice = "?";
                        }
                        ((FindPlaces) getApplicationContext()).setAll(placeName,placeLocation,placeRating,placePrice);
                    }
                } else if (result.getString(STATUS).equalsIgnoreCase(ZERO_RESULTS)) {
                    Toast.makeText(getBaseContext(), "Error: No matching restaurants found",
                            Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Toast.makeText(getBaseContext(), "Error: Cannot parse response",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
