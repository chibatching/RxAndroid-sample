package com.chibatching.rxandroid_sample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasAccessToken()){
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new TimeLineFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            // Delete access token
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit()
                    .remove(getString(R.string.key_access_token))
                    .remove(getString(R.string.key_access_token_secret))
                    .apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean hasAccessToken() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String token = sp.getString(getString(R.string.key_access_token), null);
        String secret = sp.getString(getString(R.string.key_access_token_secret), null);

        return !(token == null || secret == null);
    }
}
