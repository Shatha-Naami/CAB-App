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

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    private Button CustomerLoginButton;
    private TextView CustomerRegisterTextView;
    private TextView CustomerLinkTextView;
    private TextView CustomerStatusTextView;
    private TextInputEditText emailCustomerEditText;
    private TextInputEditText passwordCustomerEditText;

    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseRef;
    private String onlineCustomerID;

    private ProgressDialog loadingProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);

        CustomerLoginButton = findViewById(R.id.customer_login_button);
        CustomerRegisterTextView = findViewById(R.id.customer_register);
        CustomerLinkTextView = findViewById(R.id.customer_register_link);
        CustomerStatusTextView = findViewById(R.id.customer_status);
        emailCustomerEditText = findViewById(R.id.customer_email_edit_text);
        passwordCustomerEditText = findViewById(R.id.customer_password_edit_text);

        mAuth = FirebaseAuth.getInstance();

        loadingProgressDialog = new ProgressDialog(this);

        CustomerRegisterTextView.setVisibility(View.INVISIBLE);
        CustomerRegisterTextView.setEnabled(false);

        CustomerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomerLinkTextView.setVisibility(View.INVISIBLE);
                CustomerStatusTextView.setText("Register Customer");

                CustomerLoginButton.setText("Register");

            }
        });

        CustomerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailCustomerEditText.getText().toString();
                String password = passwordCustomerEditText.getText().toString();
                switch (CustomerLoginButton.getText().toString()) {
                    case "Register":
                        CustomerRegister(email, password);
                        break;
                    case "Login":
                        CustomerSingIn(email, password);
                        break;
                }

            }
        });
    }

    private void CustomerSingIn(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please Enter Email.. ", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please Enter Password.. ", Toast.LENGTH_SHORT).show();
        } else {
            loadingProgressDialog.setTitle("Customer Login");
            loadingProgressDialog.setMessage("Please wait, while we check from your data..");
            loadingProgressDialog.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                startActivity(new Intent(CustomerLoginRegisterActivity.this, CustomerMapActivity.class));
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Login Successfully..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            } else {
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Login Unsuccessful, Try again..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            }
                        }
                    });
        }
    }

    private void CustomerRegister(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please Enter Email.. ", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please Enter Password.. ", Toast.LENGTH_SHORT).show();
        } else {
            loadingProgressDialog.setTitle("Customer Registration");
            loadingProgressDialog.setMessage("Please wait, while we register your data..");
            loadingProgressDialog.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                onlineCustomerID = mAuth.getCurrentUser().getUid();
                                customerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users")
                                        .child("Customers")
                                        .child(onlineCustomerID);

                                customerDatabaseRef.setValue(true);
                                startActivity(new Intent(CustomerLoginRegisterActivity.this, CustomerMapActivity.class));

                                Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Register Successfully..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            } else {
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Registration Unsuccessful, Try again..", Toast.LENGTH_SHORT).show();
                                loadingProgressDialog.dismiss();
                            }
                        }
                    });
        }
    }
}
