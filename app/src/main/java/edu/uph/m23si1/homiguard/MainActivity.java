package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import edu.uph.m23si1.homiguard.ui.HomeFragment;
import edu.uph.m23si1.homiguard.ui.SettingFragment;
import edu.uph.m23si1.homiguard.ui.StatisticFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Default fragment saat pertama kali dibuka
        loadFragment(new HomeFragment());

        // Ganti fragment saat item di bottom nav diklik
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_statistic) {
                selectedFragment = new StatisticFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                // 🔹 panggil SettingFragment sebagai dialog
                SettingFragment settingFragment = new SettingFragment();
                settingFragment.show(getSupportFragmentManager(), "SettingFragment");
                return true; // biar nggak nge-load fragment lain
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
