package com.example.fuelmonitorapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.fuelmonitorapp.fragments.RegistrosFragment;
import com.example.fuelmonitorapp.fragments.ResumenFragment;
import com.example.fuelmonitorapp.fragments.VehiculosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.firebase.ui.auth.AuthUI;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        // Fragment inicial
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new VehiculosFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_vehiculos) {
                selectedFragment = new VehiculosFragment();
            } else if (itemId == R.id.nav_registros) {
                selectedFragment = new RegistrosFragment();
            } else if (itemId == R.id.nav_resumen) {
                selectedFragment = new ResumenFragment();
            } else if (itemId == R.id.nav_logout) {
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(task -> {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        });
                return true;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}
