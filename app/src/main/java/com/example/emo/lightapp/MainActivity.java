package com.example.emo.lightapp;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements LightFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getFragmentManager().beginTransaction()
                .replace(R.id.container01, LightFragment.newInstance())
                .commit();

        getFragmentManager().beginTransaction()
                .replace(R.id.container02, BatteryFragment.newInstance())
                .commit();
    }

    @Override
    public void onFragmentInteraction(@Nullable String[] arguments) {
        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
    }
}
