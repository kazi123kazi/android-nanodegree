package com.zn.expirytracker.settings;

import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.zn.expirytracker.R;
import com.zn.expirytracker.data.viewmodel.FoodViewModel;
import com.zn.expirytracker.ui.dialog.ConfirmDeleteDialogFragment;
import com.zn.expirytracker.utils.AuthToolbox;
import com.zn.expirytracker.utils.Toolbox;
import com.zn.expirytracker.widget.UpdateWidgetService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN;

public class SettingsFragment extends PreferenceFragmentCompat
        implements ConfirmDeleteDialogFragment.OnConfirmDeleteButtonClickListener {

    static Preference mPreferenceNotificationsNumDays;
    static Preference mPreferenceNotificationsTod;
    static Preference mPreferenceWidget;
    static Preference mPreferenceAccountSignIn;
    static Preference mPreferenceAccountSignOut;
    static Preference mPreferenceAccountSync;
    static Preference mPreferenceAccountDelete;
    static Preference mPreferenceDisplayName;
    static Preference mPreferenceWipeDeviceData;

    private static FoodViewModel mViewModel;
    private Activity mHostActivity;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(FoodViewModel.class);
        mHostActivity = getActivity();

        // For signing out and revoking access from Google authenticated accounts
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(mHostActivity, gso);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Keep a reference to preferences that will be enabled or disabled
        mPreferenceNotificationsNumDays =
                findPreference(getString(R.string.pref_notifications_days_key));
        mPreferenceNotificationsTod =
                findPreference(getString(R.string.pref_notifications_tod_key));
        mPreferenceWidget = findPreference(getString(R.string.pref_widget_num_days_key));
        mPreferenceAccountSignIn = findPreference(getString(R.string.pref_account_sign_in_key));
        mPreferenceAccountSignOut = findPreference(getString(R.string.pref_account_sign_out_key));
        mPreferenceAccountSync = findPreference(getString(R.string.pref_account_sync_key));
        mPreferenceAccountDelete = findPreference(getString(R.string.pref_account_delete_key));
        mPreferenceDisplayName = findPreference(getString(R.string.pref_account_display_name_key));
        mPreferenceWipeDeviceData = findPreference(getString(R.string.pref_account_wipe_data_key));

        // Set summaries and enabled based on switches or checkboxes
        setOnPreferenceChangeListener(findPreference(
                getString(R.string.pref_notifications_receive_key)));
        setOnPreferenceChangeListener(mPreferenceNotificationsNumDays);
        setOnPreferenceChangeListener(mPreferenceNotificationsTod);
        setOnPreferenceChangeListener(mPreferenceWidget);
        setOnPreferenceChangeListener(findPreference(
                getString(R.string.pref_widget_num_days_key)));
        setOnPreferenceChangeListener(findPreference(
                getString(R.string.pref_account_display_name_key)));

        // Set the behavior for the custom preferences
        setAccountPreferencesActions();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Show account settings only if the user is signed in
        showAccountSettings(AuthToolbox.isSignedIn());
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sOnPreferenceChangeListener
     */
    private static void setOnPreferenceChangeListener(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sOnPreferenceChangeListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof MultiSelectListPreference) {
            sOnPreferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getStringSet(preference.getKey(), new HashSet<String>()));
        } else if (preference instanceof SwitchPreference ||
                preference instanceof CheckBoxPreference) {
            sOnPreferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
            sOnPreferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    /**
     * Helper to show account settings only if user is currently logged in
     */
    private void showAccountSettings(boolean show) {
        mPreferenceDisplayName.setVisible(show);
        mPreferenceAccountSignIn.setVisible(!show);
        mPreferenceAccountSignOut.setVisible(show);
        boolean hasAccount = mPreferenceAccountSignOut.isVisible();
        mPreferenceAccountSync.setVisible(hasAccount);
        mPreferenceAccountDelete.setVisible(hasAccount);
        // TODO: Scroll automatically to the newly visible settings
    }

    /**
     * Set actions of custom preferences
     * <p>
     * https://stackoverflow.com/questions/5298370/how-to-add-a-button-to-a-preferencescreen
     */
    private void setAccountPreferencesActions() {
        mPreferenceAccountSignIn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthToolbox.startSignInActivity(mHostActivity); // Closes settings and clears back stack
                return true;
            }
        });
        mPreferenceAccountSignOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthToolbox.deleteDeviceData(mViewModel, mHostActivity);
                // Closes settings and clears back stack
                AuthToolbox.signOut(mHostActivity, mGoogleSignInClient);
                return true;
            }
        });
        mPreferenceAccountSync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO: Sync Room and Firebase here
                return false;
            }
        });
        mPreferenceAccountDelete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showWipeDataConfirmationDialog(ConfirmDeleteDialogFragment.DeleteType.ACCOUNT);
                // Delete handled in ConfirmDeleteDialogFragment.onConfirmDeleteButtonClicked
                return true;
            }
        });
        mPreferenceWipeDeviceData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showWipeDataConfirmationDialog(ConfirmDeleteDialogFragment.DeleteType.DEVICE);
                // Delete handled in ConfirmDeleteDialogFragment.onConfirmDeleteButtonClicked
                return true;
            }
        });
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value. Also adds individual preference-specific actions
     */
    private static Preference.OnPreferenceChangeListener sOnPreferenceChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    Context context = preference.getContext();
                    if (!(preference instanceof SwitchPreference ||
                            preference instanceof CheckBoxPreference)) {
                        // Only change summaries for EditTexts and lists
                        preference.setSummary(getPreferenceSummary(preference, value));
                    } else {
                        // Enable preferences based on values for SwitchPreferences and 
                        // CheckBoxPreferences
                        enablePreference(preference, value, context);
                    }

                    // Individual preference-specific actions
                    if (preference.getKey()
                            .equals(context.getString(R.string.pref_account_display_name_key))) {
                        // Sync the display name with the database. This is a logged-in only feature
                        if (AuthToolbox.isSignedIn()) {
                            String displayName = (String) value;
                            AuthToolbox.updateDisplayName(context, displayName);

                            // If there is no name, set the summary to the default
                            if (displayName.trim().isEmpty())
                                preference.setSummary(R.string.pref_account_display_name_summary);
                        }
                    } else if (preference.getKey().equals(context.getString(R.string.pref_widget_num_days_key))) {
                        // Request update
                        UpdateWidgetService.updateFoodWidget(preference.getContext());
                        return true;
                    }
                    return true;
                }
            };

    /**
     * Helper for setting the PreferenceSummary
     *
     * @param preference
     * @param value
     * @return
     */
    private static String getPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof MultiSelectListPreference) {
            // List all selected values
            List<String> selectedValues = new ArrayList<>((HashSet<String>) value);
            if (selectedValues.size() == 0) {
                // No values set
                return preference.getContext().getString(R.string.pref_multi_select_empty);
            } else {
                MultiSelectListPreference msListPreference = (MultiSelectListPreference) preference;
                List<CharSequence> summaryList = new ArrayList<>();
                CharSequence[] summaryValues = msListPreference.getEntries();
                CharSequence[] entryValues = msListPreference.getEntryValues();
                // Iterate through and only add selected values to summary
                for (int i = 0; i < entryValues.length; i++) {
                    for (String selectedValue : selectedValues) {
                        if (entryValues[i].equals(selectedValue)) {
                            summaryList.add(summaryValues[i]);
                        }
                    }
                }
                return TextUtils.join(", ", summaryList);
            }
        } else if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            return index >= 0
                    ? listPreference.getEntries()[index].toString()
                    : null;

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            return stringValue;
        }
    }

    /**
     * Helper for enabling or disabling a preference based on other preferences
     *
     * @param preference
     * @param value
     */
    private static void enablePreference(Preference preference, Object value, Context context) {
        if (preference.getKey()
                .equals(context.getString(R.string.pref_notifications_receive_key))) {
            // Enable notification settings only if notifications are enabled
            boolean enabled = (boolean) value;
            mPreferenceNotificationsNumDays.setEnabled(enabled);
            mPreferenceNotificationsTod.setEnabled(enabled);
        }
    }

    /**
     * Shows a dialog for the user to confirm wiping device data or the user account
     */
    private void showWipeDataConfirmationDialog(ConfirmDeleteDialogFragment.DeleteType deleteType) {
        ConfirmDeleteDialogFragment dialog = ConfirmDeleteDialogFragment
                .newInstance(null, false, deleteType);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), ConfirmDeleteDialogFragment.class.getSimpleName());
    }

    @Override
    public void onConfirmDeleteButtonClicked(int position, boolean isLoggedIn,
                                             ConfirmDeleteDialogFragment.DeleteType deleteType) {
        switch (position) {
            case Dialog.BUTTON_POSITIVE:
                // position or isLoggedIn does not matter here
                switch (deleteType) {
                    case ACCOUNT:
                        // this also starts the sign-in activity
                        AuthToolbox.deleteDeviceAndCloudData(mViewModel, mHostActivity);
                        // TODO: Disable all view clicks and dim the activity while this is happening
                        AuthToolbox.deleteAccount(mHostActivity, mGoogleSignInClient);
                        break;
                    case DEVICE:
                        AuthToolbox.deleteDeviceData(mViewModel, mHostActivity);
                        Toolbox.showToast(mHostActivity, "All app data deleted from device");
                        break;
                }
        }
    }
}
