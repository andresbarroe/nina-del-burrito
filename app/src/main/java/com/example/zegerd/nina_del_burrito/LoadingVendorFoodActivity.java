package com.example.zegerd.nina_del_burrito;

import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;

import java.io.Serializable;

public class LoadingVendorFoodActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressBar pb3;
    public static VendorItemAdapter item_Adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_vendor_food);

        // Init UI items
        pb3 = findViewById(R.id.progressBar3);
        pb3.setVisibility(View.VISIBLE);

        // Init FireBase stuff
        mAuth = FirebaseAuth.getInstance();

        // Pre-load adapter with DB items
        item_Adapter = new VendorItemAdapter(this, mAuth.getCurrentUser().getUid());

        // Make a small wait for the item adapter to load completely
        int secondsDelayed = 1;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new Intent(getApplicationContext(), VendorFoodActivity.class));
                finish();
            }
        }, secondsDelayed * 1000);
    }

}
