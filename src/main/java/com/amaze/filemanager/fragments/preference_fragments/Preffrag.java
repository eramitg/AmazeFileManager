/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.fragments.preference_fragments;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.AboutActivity;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.Preferences;
import com.amaze.filemanager.ui.views.CheckBox;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import static com.amaze.filemanager.R.string.feedback;

public class Preffrag extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String PREFERENCE_KEY_ABOUT = "about";
    private static final String[] PREFERENCE_KEYS =
            {"columns", "theme", "sidebar_folders_enable", "sidebar_quickaccess_enable",
                    /*"rootmode",*/"feedback", PREFERENCE_KEY_ABOUT, "plus_pic", "colors",
                    "sidebar_folders", "sidebar_quickaccess"};


    public static final String PREFERENCE_SHOW_SIDEBAR_FOLDERS = "show_sidebar_folders";
    public static final String PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES = "show_sidebar_quickaccesses";

    public static final String PREFERENCE_CRYPT_MASTER_PASSWORD = "crypt_password";
    public static final String PREFERENCE_CRYPT_FINGERPRINT = "crypt_fingerprint";
    public static final String PREFERENCE_CRYPT_WARNING_REMEMBER = "crypt_remember";

    public static final String PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT = "";
    public static final boolean PREFERENCE_CRYPT_FINGERPRINT_DEFAULT = false;
    public static final boolean PREFERENCE_CRYPT_WARNING_REMEMBER_DEFAULT = false;
    public static final String ENCRYPT_PASSWORD_FINGERPRINT = "fingerprint";
    public static final String ENCRYPT_PASSWORD_MASTER = "master";

    private UtilitiesProviderInterface utilsProvider;
    private SharedPreferences sharedPref;
    private CheckBox gplus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();

        PreferenceUtils.reset();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        for (String PREFERENCE_KEY : PREFERENCE_KEYS) {
            findPreference(PREFERENCE_KEY).setOnPreferenceClickListener(this);
        }

        gplus = (CheckBox) findPreference("plus_pic");

        if (BuildConfig.IS_VERSION_FDROID)
            gplus.setEnabled(false);

        // crypt master password
        final EditTextPreference masterPasswordPreference = (EditTextPreference) findPreference(PREFERENCE_CRYPT_MASTER_PASSWORD);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // encryption feature not available
            masterPasswordPreference.setEnabled(false);
        }

        if (sharedPref.getBoolean(PREFERENCE_CRYPT_FINGERPRINT, false)) {
            masterPasswordPreference.setEnabled(false);
        }

        // finger print sensor
        final FingerprintManager fingerprintManager = (FingerprintManager)
                getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        final KeyguardManager keyguardManager = (KeyguardManager)
                getActivity().getSystemService(Context.KEYGUARD_SERVICE);

        CheckBox checkBx = (CheckBox) findPreference(PREFERENCE_CRYPT_FINGERPRINT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && fingerprintManager.isHardwareDetected()) {
            checkBx.setEnabled(true);
        }

        checkBx.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.crypt_fingerprint_no_permission),
                            Toast.LENGTH_LONG).show();
                    return false;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !fingerprintManager.hasEnrolledFingerprints()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.crypt_fingerprint_not_enrolled),
                            Toast.LENGTH_LONG).show();
                    return false;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !keyguardManager.isKeyguardSecure()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.crypt_fingerprint_no_security),
                            Toast.LENGTH_LONG).show();
                    return false;
                }

                masterPasswordPreference.setEnabled(false);
                return true;
            }
        });
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String[] sort;
        MaterialDialog.Builder builder;

        switch (preference.getKey()) {
            case "columns":
                sort = getResources().getStringArray(R.array.columns);
                builder = new MaterialDialog.Builder(getActivity());
                builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                builder.title(R.string.gridcolumnno);
                int current = Integer.parseInt(sharedPref.getString("columns", "-1"));
                current = current == -1 ? 0 : current;
                if (current != 0) current = current - 1;
                builder.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("columns", "" + (which != 0 ? sort[which] : "" + -1)).commit();
                        dialog.dismiss();
                        return true;
                    }
                });
                builder.build().show();
                return true;
            case "theme":
                sort = getResources().getStringArray(R.array.theme);
                current = Integer.parseInt(sharedPref.getString("theme", "0"));
                builder = new MaterialDialog.Builder(getActivity());
                //builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                builder.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        utilsProvider.getThemeManager()
                                .setAppTheme(AppTheme.fromIndex(which))
                                .save();

                        Log.d("theme", AppTheme.fromIndex(which).name());

                        dialog.dismiss();
                        restartPC(getActivity());
                        return true;
                    }
                });
                builder.title(R.string.theme);
                builder.build().show();
                return true;
            case "sidebar_folders_enable":
                sharedPref.edit().putBoolean(PREFERENCE_SHOW_SIDEBAR_FOLDERS,
                        !sharedPref.getBoolean(PREFERENCE_SHOW_SIDEBAR_FOLDERS, true)).apply();
                return true;
            case "sidebar_quickaccess_enable":
                sharedPref.edit().putBoolean(PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES,
                        !sharedPref.getBoolean(PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES, true)).apply();
                return true;
            /*
            case "rootmode":
                              boolean b = sharedPref.getBoolean("rootmode", false);
                if (b) {
                    if (MainActivity.shellInteractive.isRunning()) {
                        rootmode.setChecked(true);

                    } else {  rootmode.setChecked(false);

                        Toast.makeText(getActivity(), getResources().getString(R.string.rootfailure), Toast.LENGTH_LONG).show();
                    }
                } else {
                    rootmode.setChecked(false);

                }
                return false;
            */
            case "feedback":
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "vishalmeham2@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback : Amaze File Manager");
                startActivity(Intent.createChooser(emailIntent, getResources().getString(feedback)));
                return false;
            case PREFERENCE_KEY_ABOUT:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return false;
            case "plus_pic":
                if (gplus.isChecked()) {
                    boolean b = checkGplusPermission();
                    if (!b) requestGplusPermission();
                }
                return false;
            /*FROM HERE BE FRAGMENTS*/
            case "colors":
                ((com.amaze.filemanager.activities.Preferences) getActivity())
                        .selectItem(Preferences.COLORS_PREFERENCE);
                return true;
            case "sidebar_folders":
                ((com.amaze.filemanager.activities.Preferences) getActivity())
                        .selectItem(Preferences.FOLDERS_PREFERENCE);
                return true;
            case "sidebar_quickaccess":
                ((com.amaze.filemanager.activities.Preferences) getActivity())
                        .selectItem(Preferences.QUICKACCESS_PREFERENCE);
                return true;
        }

        return false;
    }

    public static void restartPC(final Activity activity) {
        if (activity == null) return;

        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }

    public void invalidateGplus() {
        boolean a = checkGplusPermission();
        if (!a) gplus.setChecked(false);
    }

    public boolean checkGplusPermission() {
        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS)
                        == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGplusPermission() {
        final String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.INTERNET};
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.GET_ACCOUNTS) || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.INTERNET)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.

            String fab_skin = (BaseActivity.accentSkin);
            final MaterialDialog materialDialog = Futils.showBasicDialog(getActivity(), fab_skin, utilsProvider.getAppTheme(), new String[]{getResources().getString(R.string.grantgplus), getResources().getString(R.string.grantper), getResources().getString(R.string.grant), getResources().getString(R.string.cancel), null});
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat
                            .requestPermissions(getActivity(), PERMISSIONS, 66);
                    materialDialog.dismiss();
                }
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat
                    .requestPermissions(getActivity(), PERMISSIONS, 66);
        }
    }
}