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
    DatabaseHelper db;

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

        db = new DatabaseHelper(RegisterActivity.this);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String pass = etPassword.getText().toString();
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
            } else {
                // Gunakan metode saveUser dari DatabaseHelper
                // Parameter avatarUrl diberi nilai null karena belum ada input avatar
                long userId = db.saveUser(name, email, pass, null);
                if (userId != -1) {
                    Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish(); // Tutup RegisterActivity setelah registrasi berhasil
                } else {
                    Toast.makeText(this, "Gagal mendaftar, email mungkin sudah digunakan", Toast.LENGTH_SHORT).show();
                }
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