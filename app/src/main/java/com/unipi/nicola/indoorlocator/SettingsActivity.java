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
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener onChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            return true;
        }
    };

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
            startActivity(new Intent(this, WifiLocatorActivity.class));
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

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference(PREF_DISTANCE_THRESHOLD_KEY));
            //bindPreferenceSummaryToValue(findPreference(PREF_MINIMUM_MATCHING_APS_KEY));
            //bindPreferenceSummaryToValue(findPreference(PREF_NEAREST_NEIGHBORS_NUMBER_KEY));

            //bindPreferenceSummaryToValue(findPreference(PREF_STORING_ITERATIONS_KEY));
            findPreference(PREF_SIGNAL_NORMALIZATION_KEY).setOnPreferenceChangeListener(onChangeListener);
            findPreference(PREF_MINIMUM_MATCHING_APS_KEY).setOnPreferenceChangeListener(onChangeListener);
            findPreference(PREF_STORING_ITERATIONS_KEY).setOnPreferenceChangeListener(onChangeListener);
            findPreference(PREF_DISTANCE_THRESHOLD_KEY).setOnPreferenceChangeListener(onChangeListener);
            findPreference(PREF_NEAREST_NEIGHBORS_NUMBER_KEY).setOnPreferenceChangeListener(onChangeListener);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
