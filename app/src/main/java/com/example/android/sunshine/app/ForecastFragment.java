package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;


/**
 * Created by daniloplacer on 1/17/16.
 */
public class ForecastFragment extends Fragment{

    // List adapter that will be used to refresh the UI after weather data is fetched
    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    // This method is called before onCreateView
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notifies the system that this fragment has menu options,
        // so it calls onCreateOptionsMenu method later on
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // creating a new adapter with mock data above
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        new ArrayList<String>());

        // binding adapter to list view
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                // Creates a new Intent to create new Detail Activity
                // sending forecast for the selected day as extra text

                // Obs.: EXTRA_TEXT is any key. Can use any number as long we use
                // the same when retrieving it
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
                startActivity(detailIntent);
            }
        });

        // no need to call updateWeather here because its going to be called at onStart

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.forecastfragment, menu);
    }

    // Method that will call the background task to fetch weather data and update
    // the UI accordingly given a location from Shared Preferences
    public void updateWeather(){
        FetchWeatherTask task = new FetchWeatherTask();

        // Retrieves value from Shared Preferences
        // If not value was set by the user, pref.getString returns the default value
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zip = pref.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        String unit = pref.getString(getString(R.string.pref_units_key),
                getString(R.string.pref_units_default));

        task.execute(zip, unit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        // Create this constant just so if the class is renamed, do not need to change constant
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private final String BASE = "http://api.openweathermap.org/data/2.5/forecast/daily";

        @Override
        protected String[] doInBackground(String... params) {

            // Caller didn't specify a postal code
            if (params.length ==0)
                return null;

            int numDays = 7;

            Uri builder = Uri.parse(BASE).buildUpon()
                    .appendQueryParameter("q", params[0])
                    .appendQueryParameter("mode", "json")
                    .appendQueryParameter("units", params[1])
                    .appendQueryParameter("cnt",Integer.toString(numDays))
                    .appendQueryParameter("appid","cf7735f90d0d1693f7d1fdca4ad29ce4").build();

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                URL url = new URL(builder.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                forecastJsonStr = buffer.toString();

                String[] weather = WeatherDataParser.getWeatherDataFromJson(forecastJsonStr,numDays);
                return weather;

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error with JSON parsing", e);
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);

            if (strings != null){
                mForecastAdapter.clear();
                mForecastAdapter.addAll(strings);
            }
        }
    }
}