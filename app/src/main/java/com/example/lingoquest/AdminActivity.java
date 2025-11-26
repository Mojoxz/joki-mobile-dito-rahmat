package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private Spinner spinnerLanguage;
    private EditText etLevel, etQuestionText, etOption1, etOption2, etOption3, etOption4, etCorrectAnswer, etXpReward, etImageUrl;
    private Button btnSaveQuestion, btnLogout;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private List<String> languageIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        firebaseHelper = FirebaseHelper.getInstance();

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        if (userId == null || !firebaseHelper.isUserLoggedIn()) {
            Toast.makeText(this, "Akses ditolak. Silakan login terlebih dahulu.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Verify admin status
        firebaseHelper.isAdmin(userId, isAdmin -> {
            if (!isAdmin) {
                Toast.makeText(AdminActivity.this, "Akses ditolak. Hanya admin yang dapat mengakses laman ini.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

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

        loadLanguages();

        btnSaveQuestion.setOnClickListener(v -> saveQuestion());

        btnLogout.setOnClickListener(v -> {
            firebaseHelper.logoutUser();
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadLanguages() {
        List<String> languages = new ArrayList<>();
        languageIds = new ArrayList<>();

        FirebaseFirestore.getInstance()
                .collection("languages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String languageName = doc.getString("language_name");
                        if (languageName != null) {
                            languages.add(languageName);
                            languageIds.add(doc.getId());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, languages);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLanguage.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat bahasa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveQuestion() {
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

        if (spinnerLanguage.getSelectedItemPosition() < 0 || languageIds == null ||
                spinnerLanguage.getSelectedItemPosition() >= languageIds.size()) {
            Toast.makeText(this, "Pilih bahasa terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String languageId = languageIds.get(spinnerLanguage.getSelectedItemPosition());
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

        // Buat data soal
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("language_id", languageId);
        questionData.put("question_level", level);
        questionData.put("question_text", questionText);
        questionData.put("option1", option1);
        questionData.put("option2", option2);
        questionData.put("option3", option3);
        questionData.put("option4", option4);
        questionData.put("correct_answer", correctAnswer);
        questionData.put("xp_reward", xpReward);

        if (!imageUrl.isEmpty()) {
            questionData.put("image_url", imageUrl);
        }

        // Disable button saat proses save
        btnSaveQuestion.setEnabled(false);

        // Simpan ke Firebase
        firebaseHelper.addGameQuestion(questionData, new FirebaseHelper.AddQuestionCallback() {
            @Override
            public void onSuccess(String questionId) {
                btnSaveQuestion.setEnabled(true);
                Toast.makeText(AdminActivity.this, "Soal berhasil disimpan", Toast.LENGTH_SHORT).show();
                clearFields();
            }

            @Override
            public void onFailure(String error) {
                btnSaveQuestion.setEnabled(true);
                Toast.makeText(AdminActivity.this, "Gagal menyimpan soal: " + error, Toast.LENGTH_SHORT).show();
            }
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
}