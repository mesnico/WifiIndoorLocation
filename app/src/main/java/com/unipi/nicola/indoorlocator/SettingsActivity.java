package com.unipi.nicola.indoorlocator;


import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceManager;
import android.view.MenuItem;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String PREF_SIGNAL_NORMALIZATION_KEY = "signal_normalization";
    public static final String PREF_DISTANCE_THRESHOLD_KEY = "distance_threshold";
    public static final String PREF_MINIMUM_MATCHING_APS_KEY = "min_matching_aps";
    public static final String PREF_NEAREST_NEIGHBORS_NUMBER_KEY = "nn_number";

    public static final String PREF_STORING_ITERATIONS_KEY = "storing_iterations";

    public static final String PREF_BETA = "beta";
    public static final String PREF_STEP_LENGTH = "step_length";
    public static final String PREF_UPDATE_AFTER_NUM_STEPS = "update_after_num_steps";

    public static final String PREF_SHOW_REAL_POSITION = "show_real_position";

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferenceFragment()).commit();
        setupActionBar();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            /*Intent activity = new Intent(this, WifiLocatorActivity.class);
            activity.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            startActivity(activity);*/
            onBackPressed();
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * This fragment shows location matching preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferenceFragment extends android.preference.PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_fragment);
            setHasOptionsMenu(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                //startActivity(new Intent(getActivity(), SettingsActivity.class));
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
