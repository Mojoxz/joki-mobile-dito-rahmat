package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvToRegister;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailInput);
        etPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.loginButton);
        tvToRegister = findViewById(R.id.tvToRegister);
        db = new DatabaseHelper(LoginActivity.this);

        // Cek apakah sudah login
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            if (db.isAdmin(userId)) {
                startActivity(new Intent(this, AdminActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
            } else {
                Cursor cursor = db.getReadableDatabase().rawQuery(
                        "SELECT * FROM " + DatabaseHelper.TABLE_USERS +
                                " WHERE " + DatabaseHelper.COLUMN_EMAIL + " = ? AND " +
                                DatabaseHelper.COLUMN_PASSWORD + " = ?",
                        new String[]{email, password});

                if (cursor.moveToFirst()) {
                    int userIdFromDb = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID));
                    int isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_ADMIN));

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("user_id", userIdFromDb);
                    editor.apply();

                    if (isAdmin == 1) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                } else {
                    Toast.makeText(this, "Email atau kata sandi salah", Toast.LENGTH_SHORT).show();
                }
                cursor.close();
            }
        });

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}