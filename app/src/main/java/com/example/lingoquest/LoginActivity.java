package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvToRegister;
    FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        tvToRegister = findViewById(R.id.tvToRegister);
        firebaseHelper = FirebaseHelper.getInstance();

        // Cek apakah sudah login
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId != null && firebaseHelper.isUserLoggedIn()) {
            // Check if admin
            firebaseHelper.isAdmin(userId, isAdmin -> {
                if (isAdmin) {
                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            });
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
            } else {
                // Disable button saat proses login
                btnLogin.setEnabled(false);

                firebaseHelper.loginUser(email, password, new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        // Simpan user_id ke SharedPreferences
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_id", userId);
                        editor.apply();

                        // Check if admin
                        firebaseHelper.isAdmin(userId, isAdmin -> {
                            btnLogin.setEnabled(true);
                            if (isAdmin) {
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Email atau kata sandi salah", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}