package com.example.cab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginRegisterActivity extends AppCompatActivity {

    private Button DriverLoginButton;
    private TextView DriverRegisterTextView;
    private TextView DriverLinkTextView;
    private TextView DriverStatusTextView;
    private TextInputEditText emailDriverEditText;
    private TextInputEditText passwordDriverEditText;

    private FirebaseAuth mAuth;
    private DatabaseReference driverDatabaseRef;
    private String onlineDriverID;

    private ProgressDialog loadingProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);

        DriverLoginButton = findViewById(R.id.driver_login_button);
        DriverRegisterTextView = findViewById(R.id.driver_register);
        DriverLinkTextView = findViewById(R.id.driver_register_link);
        DriverStatusTextView = findViewById(R.id.driver_status);
        emailDriverEditText = findViewById(R.id.driver_email_edit_text);
        passwordDriverEditText = findViewById(R.id.driver_password_edit_text);

        mAuth = FirebaseAuth.getInstance();

        loadingProgressDialog = new ProgressDialog(this);

        DriverRegisterTextView.setVisibility(View.INVISIBLE);
        DriverRegisterTextView.setEnabled(false);

        DriverLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DriverLinkTextView.setVisibility(View.INVISIBLE);
                DriverStatusTextView.setText("Register Driver");

                DriverLoginButton.setText("Register");

            }
        });

        DriverLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailDriverEditText.getText().toString();
                String password = passwordDriverEditText.getText().toString();
                switch (DriverLoginButton.getText().toString()) {
                    case "Register":
                        DriverRegister(email, password);
                        break;
                    case "Login":
                        DriverSingIn(email, password);
                        break;
                }
            }
        });
    }

    private void DriverSingIn(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please Enter Email.. ", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please Enter Password.. ", Toast.LENGTH_SHORT).show();
        } else {
            loadingProgressDialog.setTitle("Driver Login");
            loadingProgressDialog.setMessage("Please wait, while we check from your data..");
            loadingProgressDialog.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                startActivity(new Intent(DriverLoginRegisterActivity.this, DriverMapActivity.class));
                                Toast.makeText(DriverLoginRegisterActivity.this, "Driver Login Successfully..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            } else {
                                Toast.makeText(DriverLoginRegisterActivity.this, "Login Unsuccessful, Try again..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            }
                        }
                    });
        }
    }

    private void DriverRegister(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please Enter Email.. ", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please Enter Password.. ", Toast.LENGTH_SHORT).show();
        } else {
            loadingProgressDialog.setTitle("Driver Registration");
            loadingProgressDialog.setMessage("Please wait, while we register your data..");
            loadingProgressDialog.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                onlineDriverID = mAuth.getCurrentUser().getUid();
                                driverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users")
                                        .child("Drivers")
                                        .child(onlineDriverID);

                                driverDatabaseRef.setValue(true);
                                startActivity(new Intent(DriverLoginRegisterActivity.this, DriverMapActivity.class));

                                Toast.makeText(DriverLoginRegisterActivity.this, "Driver Register Successfully..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            } else {
                                Toast.makeText(DriverLoginRegisterActivity.this, "Registration Unsuccessful, Try again..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            }
                        }
                    });
        }
    }
}
