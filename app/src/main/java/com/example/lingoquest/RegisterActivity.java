package com.example.lingoquest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword;
    Button btnRegister;
    TextView tvToLogin;
    MaterialButton btnGoogleRegister, btnFacebookRegister;
    FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.nameInput);
        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnRegister = findViewById(R.id.registerButton);
        tvToLogin = findViewById(R.id.tvToLogin);
        btnGoogleRegister = findViewById(R.id.googleRegisterButton);
        btnFacebookRegister = findViewById(R.id.facebookRegisterButton);

        firebaseHelper = FirebaseHelper.getInstance();

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
            } else {
                // Disable button saat proses registrasi
                btnRegister.setEnabled(false);

                // Gunakan FirebaseHelper untuk registrasi
                // avatarUrl diberi nilai null karena belum ada input avatar
                firebaseHelper.registerUser(email, pass, name, null, new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Registrasi berhasil", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish(); // Tutup RegisterActivity setelah registrasi berhasil
                    }

                    @Override
                    public void onFailure(String error) {
                        btnRegister.setEnabled(true);
                        if (error.contains("already in use") || error.contains("email-already-in-use")) {
                            Toast.makeText(RegisterActivity.this, "Gagal mendaftar, email mungkin sudah digunakan", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Gagal mendaftar: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        tvToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnGoogleRegister.setOnClickListener(v -> {
            // Nanti diganti dengan login Google asli
            Toast.makeText(this, "Daftar dengan Google belum diimplementasikan", Toast.LENGTH_SHORT).show();
        });

        btnFacebookRegister.setOnClickListener(v -> {
            // Nanti diganti dengan login Facebook asli
            Toast.makeText(this, "Daftar dengan Facebook belum diimplementasikan", Toast.LENGTH_SHORT).show();
        });
    }
}