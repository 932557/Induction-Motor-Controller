package com.example.induction_motor_controller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private EditText editTextEmail, editTextPassword, editTextMotorName, editTextUserName;
    private Button registerBtn;
    TextView login_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        onClick();
    }

//    initializing views
    private void initViews() {
        editTextUserName = findViewById(R.id.editTextUserName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextMotorName = findViewById(R.id.editTextMotorName);
        registerBtn = findViewById(R.id.buttonRegister);
        login_txt = findViewById(R.id.textViewLogin);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
    }

//    applying event listeners on button or text click's
    private void onClick() {
        registerBtn.setOnClickListener(view -> {
            registerUser();
        });

        login_txt.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String userName = editTextUserName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String motorName = editTextMotorName.getText().toString().trim();

        // Add your registration logic here
        // Validate fields
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(motorName) ||
                TextUtils.isEmpty(userName)) {
            showToast("Please fill in all fields.");
            return;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters long.");
            return;
        }

        // Register user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            // Save user data to Firebase Realtime Database
                            saveUserDataToDatabase(userName, userId, email, motorName);
                            showToast("User registered successfully.");

//                            after saving start main activity and finish register activity
                            Intent intentRegisterUser = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intentRegisterUser);
                            finish(); // Finish activity after registration
                        }
                    } else {
                        showToast("Registration failed. Please try again.");
                    }
                });

    }

//    saving registered user to database
    private void saveUserDataToDatabase(String userName, String userId, String email, String motorName) {
        List<DailyLogPojo> dailyLogList = new ArrayList<>();
        UserPojo user = new UserPojo(userName, email, motorName, dailyLogList);
        mDatabase.child(userId).setValue(user);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}