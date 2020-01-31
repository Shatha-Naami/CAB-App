package com.example.cab;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomActivity extends AppCompatActivity {

    private Button driverButton;
    private Button customerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcom);

        driverButton = findViewById(R.id.driver_button);
        customerButton = findViewById(R.id.custom_button);

        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomActivity.this, DriverLoginRegisterActivity.class));
            }
        });

        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomActivity.this, CustomerLoginRegisterActivity.class));
            }
        });
    }
}
