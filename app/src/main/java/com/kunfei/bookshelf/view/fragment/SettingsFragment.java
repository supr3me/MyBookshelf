package com.kunfei.bookshelf.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.hwangjr.rxbus.RxBus;
import com.kunfei.bookshelf.MApplication;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.help.FileHelp;
import com.kunfei.bookshelf.help.RxBusTag;
import com.kunfei.bookshelf.utils.FileUtil;
import com.kunfei.bookshelf.utils.PermissionUtils;
import com.kunfei.bookshelf.view.activity.SettingActivity;

import java.util.Objects;

import cn.qqtheme.framework.picker.FilePicker;

/**
 * Created by GKF on 2017/12/16.
 * 设置
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SettingActivity settingActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("CONFIG");
        addPreferencesFromResource(R.xml.pref_settings);
        settingActivity = (SettingActivity) this.getActivity();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (Objects.equals(sharedPreferences.getString(getString(R.string.pk_download_path), ""), "")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pk_download_path), FileHelp.getCachePath());
            editor.apply();
        }
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pk_bookshelf_px)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pk_download_path)));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (Preference preference, Object value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            // Set the summary to reflect the new value.
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else {
            // For all other preferences, set the summary to the value's
            preference.setSummary(stringValue);
        }
        return true;
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                preference.getContext().getSharedPreferences("CONFIG", Context.MODE_PRIVATE).getString(preference.getKey(), ""));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pk_bookshelf_px))) {
            RxBus.get().post(RxBusTag.RECREATE, true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(getString(R.string.pk_download_path))) {
            selectDownloadPath(preference);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void selectDownloadPath(Preference preference) {
        PermissionUtils.checkMorePermissions(getActivity(), MApplication.PerList, new PermissionUtils.PermissionCheckCallBack() {
            @Override
            public void onHasPermission() {
                FilePicker picker = new FilePicker(getActivity(), FilePicker.DIRECTORY);
                picker.setBackgroundColor(getResources().getColor(R.color.background));
                picker.setTopBackgroundColor(getResources().getColor(R.color.background));
                picker.setRootPath(preference.getSummary().toString());
                picker.setItemHeight(30);
                picker.setOnFilePickListener(currentPath -> {
                    if (!currentPath.contains(FileUtil.getSdCardPath())) {
                        MApplication.getInstance().setDownloadPath(FileHelp.getCachePath());
                    } else {
                        MApplication.getInstance().setDownloadPath(currentPath);
                    }
                    preference.setSummary(MApplication.downloadPath);
                });
                picker.show();
                picker.getCancelButton().setText("恢复默认");
                picker.getCancelButton().setOnClickListener(view -> {
                    picker.dismiss();
                    MApplication.getInstance().setDownloadPath(FileHelp.getCachePath());
                    preference.setSummary(MApplication.downloadPath);
                });
            }

            @Override
            public void onUserHasAlreadyTurnedDown(String... permission) {
                Toast.makeText(getActivity(), "自定义缓存路径需要存储权限", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserHasAlreadyTurnedDownAndDontAsk(String... permission) {
                PermissionUtils.requestMorePermissions(getActivity(), MApplication.PerList, MApplication.RESULT__PERMS);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
