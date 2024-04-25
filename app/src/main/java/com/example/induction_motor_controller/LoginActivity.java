package com.example.induction_motor_controller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth mAuth;
    private MaterialButton buttonLogin;
    TextView register_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        onClick();
    }

//    initializing views
    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        register_txt = findViewById(R.id.textViewCreateAccount);

        mAuth = FirebaseAuth.getInstance();
    }

//    assign click listeners
    private void onClick() {
//        when login button click
        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            loginUser(email, password);
        });

//        when "don't have account register click" text click
        register_txt.setOnClickListener(view -> {
//            starting register new user screen and finishing login screen
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loginUser(String email, String password) {
        // Validate fields if it is empty
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("Please fill in all fields.", Toast.LENGTH_SHORT);
            return;
        }

        // Authenticate user using Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, redirect to MainActivity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish(); // Finish LoginActivity to prevent returning back
                    } else {
                        // Login failed, show error message
                        showToast("Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT);
                    }
                });
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }
}