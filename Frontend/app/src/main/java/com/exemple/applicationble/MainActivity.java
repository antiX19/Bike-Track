package com.exemple.applicationble;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_home) {
                // Handle Home action
            } else if (id == R.id.nav_profile) {
                // Handle Profile action
            } else if (id == R.id.nav_settings) {
                // Handle Settings action
            } else if (id == R.id.nav_logout) {
                // Handle Logout action
            }

            // Close the drawer after an item is clicked
            drawerLayout.closeDrawer(navigationView);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        // Close the drawer if it's open, otherwise proceed with back press
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
}
