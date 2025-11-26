package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private Spinner spinnerLanguage;
    private EditText etLevel, etQuestionText, etOption1, etOption2, etOption3, etOption4, etCorrectAnswer, etXpReward, etImageUrl;
    private Button btnSaveQuestion, btnLogout;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1 || !db.isAdmin(userId)) {
            Toast.makeText(this, "Akses ditolak. Hanya admin yang dapat mengakses laman ini.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etLevel = findViewById(R.id.etLevel);
        etQuestionText = findViewById(R.id.etQuestionText);
        etOption1 = findViewById(R.id.etOption1);
        etOption2 = findViewById(R.id.etOption2);
        etOption3 = findViewById(R.id.etOption3);
        etOption4 = findViewById(R.id.etOption4);
        etCorrectAnswer = findViewById(R.id.etCorrectAnswer);
        etXpReward = findViewById(R.id.etXpReward);
        etImageUrl = findViewById(R.id.etImageUrl);
        btnSaveQuestion = findViewById(R.id.btnSaveQuestion);
        btnLogout = findViewById(R.id.btnLogout);

        List<String> languages = new ArrayList<>();
        List<Integer> languageIds = new ArrayList<>();
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_LANGUAGES, null);
        while (cursor.moveToNext()) {
            languages.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_NAME)));
            languageIds.add(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_ID)));
        }
        cursor.close();
        readableDb.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        btnSaveQuestion.setOnClickListener(v -> {
            String levelStr = etLevel.getText().toString();
            String questionText = etQuestionText.getText().toString();
            String option1 = etOption1.getText().toString();
            String option2 = etOption2.getText().toString();
            String option3 = etOption3.getText().toString();
            String option4 = etOption4.getText().toString();
            String correctAnswer = etCorrectAnswer.getText().toString();
            String xpRewardStr = etXpReward.getText().toString();
            String imageUrl = etImageUrl.getText().toString();

            if (levelStr.isEmpty() || questionText.isEmpty() || option1.isEmpty() || option2.isEmpty() ||
                    option3.isEmpty() || option4.isEmpty() || correctAnswer.isEmpty() || xpRewardStr.isEmpty()) {
                Toast.makeText(this, "Isi semua field yang wajib", Toast.LENGTH_SHORT).show();
                return;
            }

            int languageId = languageIds.get(spinnerLanguage.getSelectedItemPosition());
            int level;
            int xpReward;
            try {
                level = Integer.parseInt(levelStr);
                xpReward = Integer.parseInt(xpRewardStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Level dan XP Reward harus berupa angka", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!correctAnswer.equals(option1) && !correctAnswer.equals(option2) &&
                    !correctAnswer.equals(option3) && !correctAnswer.equals(option4)) {
                Toast.makeText(this, "Jawaban benar harus sama dengan salah satu pilihan", Toast.LENGTH_SHORT).show();
                return;
            }

            long result = db.saveGameQuestion(languageId, level, questionText, option1, option2, option3, option4,
                    correctAnswer, xpReward, imageUrl.isEmpty() ? null : imageUrl);
            if (result != -1) {
                Toast.makeText(this, "Soal berhasil disimpan", Toast.LENGTH_SHORT).show();
                clearFields();
            } else {
                Toast.makeText(this, "Gagal menyimpan soal", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("user_id");
            editor.apply();
            Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void clearFields() {
        etLevel.setText("");
        etQuestionText.setText("");
        etOption1.setText("");
        etOption2.setText("");
        etOption3.setText("");
        etOption4.setText("");
        etCorrectAnswer.setText("");
        etXpReward.setText("");
        etImageUrl.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}