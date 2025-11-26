package com.example.lingoquest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;

import android.content.ContentValues;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfilActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;

    private ImageView ivProfilePicture, ivEditIcon;
    private TextView tvUsername, tvLevelXp, tvXpToNextLevel, tvWordsLearned, tvModulesCompleted, tvChallengesToday, tvTotalXp;
    private ProgressBar progressBar;
    private CardView cvBadgePemula, cvBadgeRajin;
    private LinearLayout llEditProfile, llChangePassword, llLogout;
    private DatabaseHelper dbHelper;
    private int userId;
    private static final int MAX_LEVEL = 50;
    private static final int XP_PER_LEVEL = 1000;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    updateProfilePicture(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateProfilePicture(cameraImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_profile);

        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ivProfilePicture = findViewById(R.id.profile_picture);
        ivEditIcon = findViewById(R.id.edit_icon);
        tvUsername = findViewById(R.id.username);
        tvLevelXp = findViewById(R.id.level_xp);
        tvXpToNextLevel = findViewById(R.id.xp_to_next_level);
        progressBar = findViewById(R.id.progressBar);
        tvWordsLearned = findViewById(R.id.words_learned);
        tvModulesCompleted = findViewById(R.id.modules_completed);
        tvChallengesToday = findViewById(R.id.challenges_today);
        tvTotalXp = findViewById(R.id.total_xp); // Tambahkan inisialisasi untuk total_xp
        cvBadgePemula = findViewById(R.id.badge_pemula);
        cvBadgeRajin = findViewById(R.id.badge_rajin);
        llEditProfile = findViewById(R.id.edit_profile_layout);
        llChangePassword = findViewById(R.id.change_password_layout);
        llLogout = findViewById(R.id.logout_layout);

        loadUserProfile();
        loadUserStats();
        loadAchievements();

        ivEditIcon.setOnClickListener(v -> showImagePickerDialog());
        llEditProfile.setOnClickListener(v -> showEditProfileDialog());
        llChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        llLogout.setOnClickListener(v -> logout());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile); // Set "Profil" sebagai aktif
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;
            switch (navItem) {
                case NAV_HOME:
                    startActivity(new Intent(ProfilActivity.this, MainActivity.class));
                    return true;
                case NAV_BELAJAR:
                    startActivity(new Intent(ProfilActivity.this, BelajarActivity.class));
                    return true;
                case NAV_TANTANGAN:
                    startActivity(new Intent(ProfilActivity.this, TantanganActivity.class));
                    return true;
                case NAV_PERINGKAT:
                    startActivity(new Intent(ProfilActivity.this, PeringkatActivity.class));
                    return true;
                case NAV_PROFIL:
                    return true;
                default:
                    return false;
            }
        });
    }

    private void loadUserProfile() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AVATAR_URL));
            tvUsername.setText(username);
            loadProfileImage(avatarUrl);
        }
        cursor.close();

        Cursor statsCursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_LEVEL + ", " + DatabaseHelper.COLUMN_POINTS +
                " FROM " + DatabaseHelper.TABLE_USER_STATS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (statsCursor.moveToFirst()) {
            int level = statsCursor.getInt(statsCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LEVEL));
            int totalXp = statsCursor.getInt(statsCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POINTS));
            tvLevelXp.setText("Level " + level + " • " + totalXp + " XP");
            tvTotalXp.setText(String.format("%,d", totalXp)); // Perbarui total_xp di statistik

            int xpForCurrentLevel = (level - 1) * XP_PER_LEVEL;
            int xpForNextLevel = level * XP_PER_LEVEL;
            int xpProgress = totalXp - xpForCurrentLevel;
            int xpNeeded = xpForNextLevel - xpForCurrentLevel;
            int progressPercentage = (xpProgress * 100) / xpNeeded;
            progressBar.setProgress(progressPercentage);
            tvXpToNextLevel.setText((xpNeeded - xpProgress) + " XP to next level");
        }
        statsCursor.close();
        db.close();
    }

    private void loadProfileImage(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                File file = new File(avatarUrl);
                if (file.exists()) {
                    ivProfilePicture.setImageURI(Uri.fromFile(file));
                } else {
                    ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (Exception e) {
                ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery);
                Log.e("ProfilActivity", "Error loading image: " + e.getMessage());
            }
        } else {
            ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Ambil Foto", "Pilih dari Galeri"};
        new AlertDialog.Builder(this)
                .setTitle("Pilih Gambar Profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        } else {
                            openCamera();
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                        } else {
                            openGallery();
                        }
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = new File(getFilesDir(), "profile_" + userId + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, "com.example.lingoquest.fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfilePicture(Uri imageUri) {
        String avatarPath = dbHelper.saveImageToInternalStorage(this, imageUri, userId);
        if (avatarPath != null) {
            dbHelper.updateUserAvatar(userId, avatarPath);
            loadProfileImage(avatarPath);
            Toast.makeText(this, "Foto profil diperbarui", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Gagal memperbarui foto profil", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Kata Dipelajari: Jumlah keseluruhan soal yang sudah dikerjakan (jawaban benar)
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_CORRECT_ANSWERS +
                " FROM " + DatabaseHelper.TABLE_USER_STATS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            int correctAnswers = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CORRECT_ANSWERS));
            tvWordsLearned.setText(String.format("%,d", correctAnswers)); // Format dengan pemisah ribuan
        } else {
            tvWordsLearned.setText("0");
        }
        cursor.close();

        // 2. Modul Selesai: Jumlah bahasa yang progresnya 100% (level maksimum tercapai)
        int modulesCompleted = 0;
        Cursor langCursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USER_LANGUAGES +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        while (langCursor.moveToNext()) {
            Cursor progressCursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_CURRENT_LEVEL +
                            " FROM " + DatabaseHelper.TABLE_USER_GAME_PROGRESS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?",
                    new String[]{String.valueOf(userId), String.valueOf(langCursor.getInt(langCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_ID)))});
            if (progressCursor.moveToFirst()) {
                int currentLevel = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
                if (currentLevel >= MAX_LEVEL) { // Progres 100% jika level maksimum tercapai
                    modulesCompleted++;
                }
            }
            progressCursor.close();
        }
        langCursor.close();
        tvModulesCompleted.setText(String.valueOf(modulesCompleted));

        // 3. Tantangan Harian: Jumlah tantangan harian yang sudah diselesaikan pada hari ini
        int challengesToday = 0;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor challengeCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DAILY_MISSIONS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_DAILY_PROGRESS + " >= 10 AND " +
                        "date(" + DatabaseHelper.COLUMN_LAST_UPDATED + ") = ?",
                new String[]{String.valueOf(userId), today});
        if (challengeCursor.moveToFirst()) {
            challengesToday = challengeCursor.getInt(0); // Jumlah misi harian yang selesai hari ini
        }
        challengeCursor.close();
        tvChallengesToday.setText(String.valueOf(challengesToday));
    }

    private void loadAchievements() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor langCursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USER_LANGUAGES +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        boolean hasCompletedModule = false;
        while (langCursor.moveToNext()) {
            Cursor progressCursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_CURRENT_LEVEL +
                            " FROM " + DatabaseHelper.TABLE_USER_GAME_PROGRESS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?",
                    new String[]{String.valueOf(userId), String.valueOf(langCursor.getInt(langCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_ID)))});
            if (progressCursor.moveToFirst()) {
                int currentLevel = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
                if (currentLevel >= 5) { // Badge Pemula: minimal level 5
                    hasCompletedModule = true;
                }
            }
            progressCursor.close();
        }
        langCursor.close();
        cvBadgePemula.setVisibility(hasCompletedModule ? View.VISIBLE : View.GONE);

        Cursor statsCursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_STREAK_DAYS +
                " FROM " + DatabaseHelper.TABLE_USER_STATS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        boolean hasStreak = false;
        if (statsCursor.moveToFirst()) {
            int streakDays = statsCursor.getInt(statsCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STREAK_DAYS));
            if (streakDays >= 7) { // Badge Rajin: streak minimal 7 hari
                hasStreak = true;
            }
        }
        statsCursor.close();
        cvBadgeRajin.setVisibility(hasStreak ? View.VISIBLE : View.GONE);

        db.close();
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        TextView etUsername = dialogView.findViewById(R.id.et_username);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_USERNAME +
                " FROM " + DatabaseHelper.TABLE_USERS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            String currentUsername = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
            etUsername.setText(currentUsername);
        }
        cursor.close();
        db.close();

        builder.setTitle("Edit Profil")
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newUsername = etUsername.getText().toString().trim();
                    if (newUsername.isEmpty()) {
                        Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase writeDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_USERNAME, newUsername);
                    int rowsAffected = writeDb.update(DatabaseHelper.TABLE_USERS, values,
                            DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
                    writeDb.close();

                    if (rowsAffected > 0) {
                        tvUsername.setText(newUsername);
                        Toast.makeText(this, "Profil diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextView etOldPassword = dialogView.findViewById(R.id.et_old_password);
        TextView etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextView etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        builder.setTitle("Ganti Kata Sandi")
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String oldPassword = etOldPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "Kata sandi baru tidak cocok", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_PASSWORD +
                            " FROM " + DatabaseHelper.TABLE_USERS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
                    if (cursor.moveToFirst()) {
                        String currentPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD));
                        if (!currentPassword.equals(oldPassword)) {
                            Toast.makeText(this, "Kata sandi lama salah", Toast.LENGTH_SHORT).show();
                            cursor.close();
                            db.close();
                            return;
                        }
                    }
                    cursor.close();
                    db.close();

                    SQLiteDatabase writeDb = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
                    int rowsAffected = writeDb.update(DatabaseHelper.TABLE_USERS, values,
                            DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
                    writeDb.close();

                    if (rowsAffected > 0) {
                        Toast.makeText(this, "Kata sandi diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Gagal memperbarui kata sandi", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.apply();

        Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}