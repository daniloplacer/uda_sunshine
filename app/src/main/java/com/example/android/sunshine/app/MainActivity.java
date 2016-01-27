package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
           getSupportFragmentManager().beginTransaction().add(R.id.container, new ForecastFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);

            return true;
            // Opens the user prefered location on a map
        } else if (id == R.id.action_map) {

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String zip = pref.getString(getString(R.string.pref_location_key),
                    getString(R.string.pref_location_default));

            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
            Uri builder = Uri.parse("geo:00").buildUpon()
                    .appendQueryParameter("q",zip).build();
            mapIntent.setData(builder);

            if (mapIntent.resolveActivity(this.getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
