package com.example.lingoquest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfilActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;

    private ImageView ivProfilePicture, ivEditIcon;
    private TextView tvUsername, tvLevelXp, tvXpToNextLevel, tvWordsLearned, tvModulesCompleted, tvChallengesToday, tvTotalXp;
    private ProgressBar progressBar;
    private CardView cvBadgePemula, cvBadgeRajin;
    private LinearLayout llEditProfile, llChangePassword, llLogout;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String userId;
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

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        if (userId == null) {
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
        tvTotalXp = findViewById(R.id.total_xp);
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
        bottomNav.setSelectedItemId(R.id.nav_profile);
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
        // Load user data
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.child("username").getValue(String.class);
                String avatarUrl = dataSnapshot.child("avatar_url").getValue(String.class);

                if (username != null) {
                    tvUsername.setText(username);
                }
                loadProfileImage(avatarUrl);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProfilActivity", "Error loading user: " + databaseError.getMessage());
            }
        });

        // Load user stats
        mDatabase.child("user_stats").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer level = dataSnapshot.child("level").getValue(Integer.class);
                Integer totalXp = dataSnapshot.child("points").getValue(Integer.class);

                if (level == null) level = 1;
                if (totalXp == null) totalXp = 0;

                tvLevelXp.setText("Level " + level + " • " + totalXp + " XP");
                tvTotalXp.setText(String.format("%,d", totalXp));

                int xpForCurrentLevel = (level - 1) * XP_PER_LEVEL;
                int xpForNextLevel = level * XP_PER_LEVEL;
                int xpProgress = totalXp - xpForCurrentLevel;
                int xpNeeded = xpForNextLevel - xpForCurrentLevel;
                int progressPercentage = (xpProgress * 100) / xpNeeded;
                progressBar.setProgress(progressPercentage);
                tvXpToNextLevel.setText((xpNeeded - xpProgress) + " XP to next level");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProfilActivity", "Error loading stats: " + databaseError.getMessage());
            }
        });
    }

    private void loadProfileImage(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                // Cek apakah URL lokal atau Firebase Storage
                if (avatarUrl.startsWith("gs://") || avatarUrl.startsWith("https://")) {
                    // Firebase Storage URL - bisa ditambahkan Glide/Picasso untuk load dari URL
                    ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    // Local file
                    File file = new File(avatarUrl);
                    if (file.exists()) {
                        ivProfilePicture.setImageURI(Uri.fromFile(file));
                    } else {
                        ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
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
        if (imageUri != null) {
            // Upload ke Firebase Storage
            StorageReference fileRef = mStorage.child("profile_images/" + userId + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            // Update avatar_url di Firebase Database
                            mDatabase.child("users").child(userId).child("avatar_url").setValue(downloadUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        loadProfileImage(downloadUrl);
                                        Toast.makeText(this, "Foto profil diperbarui", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Gagal memperbarui database", Toast.LENGTH_SHORT).show();
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal upload foto", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadUserStats() {
        // 1. Kata Dipelajari: Jumlah jawaban benar
        mDatabase.child("user_stats").child(userId).child("correct_answers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer correctAnswers = dataSnapshot.getValue(Integer.class);
                        if (correctAnswers == null) correctAnswers = 0;
                        tvWordsLearned.setText(String.format("%,d", correctAnswers));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        tvWordsLearned.setText("0");
                    }
                });

        // 2. Modul Selesai: Jumlah bahasa dengan level maksimum
        mDatabase.child("user_languages").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int modulesCompleted = 0;
                for (DataSnapshot langSnapshot : dataSnapshot.getChildren()) {
                    String languageId = langSnapshot.getKey();
                    // Check progress untuk bahasa ini
                    checkLanguageProgress(languageId, isCompleted -> {
                        // Implementasi counter di sini
                    });
                }
                // Untuk simplifikasi, kita hitung dari user_game_progress
                countCompletedModules();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvModulesCompleted.setText("0");
            }
        });

        // 3. Tantangan Harian: Misi yang selesai hari ini
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        mDatabase.child("daily_missions").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int challengesToday = 0;
                for (DataSnapshot missionSnapshot : dataSnapshot.getChildren()) {
                    Integer progress = missionSnapshot.child("daily_progress").getValue(Integer.class);
                    String lastUpdated = missionSnapshot.child("last_updated").getValue(String.class);

                    if (progress != null && progress >= 10 && lastUpdated != null && lastUpdated.startsWith(today)) {
                        challengesToday++;
                    }
                }
                tvChallengesToday.setText(String.valueOf(challengesToday));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvChallengesToday.setText("0");
            }
        });
    }

    private void countCompletedModules() {
        mDatabase.child("user_game_progress").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int modulesCompleted = 0;
                for (DataSnapshot langSnapshot : dataSnapshot.getChildren()) {
                    Integer currentLevel = langSnapshot.child("current_level").getValue(Integer.class);
                    if (currentLevel != null && currentLevel >= MAX_LEVEL) {
                        modulesCompleted++;
                    }
                }
                tvModulesCompleted.setText(String.valueOf(modulesCompleted));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvModulesCompleted.setText("0");
            }
        });
    }

    private void checkLanguageProgress(String languageId, ProgressCallback callback) {
        mDatabase.child("user_game_progress").child(userId).child(languageId).child("current_level")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer level = dataSnapshot.getValue(Integer.class);
                        callback.onProgressChecked(level != null && level >= MAX_LEVEL);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onProgressChecked(false);
                    }
                });
    }

    private void loadAchievements() {
        // Badge Pemula: minimal level 5 di salah satu bahasa
        mDatabase.child("user_game_progress").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasCompletedModule = false;
                for (DataSnapshot langSnapshot : dataSnapshot.getChildren()) {
                    Integer currentLevel = langSnapshot.child("current_level").getValue(Integer.class);
                    if (currentLevel != null && currentLevel >= 5) {
                        hasCompletedModule = true;
                        break;
                    }
                }
                cvBadgePemula.setVisibility(hasCompletedModule ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                cvBadgePemula.setVisibility(View.GONE);
            }
        });

        // Badge Rajin: streak minimal 7 hari
        mDatabase.child("user_stats").child(userId).child("streak_days")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer streakDays = dataSnapshot.getValue(Integer.class);
                        boolean hasStreak = streakDays != null && streakDays >= 7;
                        cvBadgeRajin.setVisibility(hasStreak ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        cvBadgeRajin.setVisibility(View.GONE);
                    }
                });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        TextView etUsername = dialogView.findViewById(R.id.et_username);

        mDatabase.child("users").child(userId).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String currentUsername = dataSnapshot.getValue(String.class);
                        if (currentUsername != null) {
                            etUsername.setText(currentUsername);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });

        builder.setTitle("Edit Profil")
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newUsername = etUsername.getText().toString().trim();
                    if (newUsername.isEmpty()) {
                        Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mDatabase.child("users").child(userId).child("username").setValue(newUsername)
                            .addOnSuccessListener(aVoid -> {
                                tvUsername.setText(newUsername);
                                Toast.makeText(this, "Profil diperbarui", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
                            });
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

                    mDatabase.child("users").child(userId).child("password")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String currentPassword = dataSnapshot.getValue(String.class);
                                    if (currentPassword == null || !currentPassword.equals(oldPassword)) {
                                        Toast.makeText(ProfilActivity.this, "Kata sandi lama salah", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    mDatabase.child("users").child(userId).child("password").setValue(newPassword)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(ProfilActivity.this, "Kata sandi diperbarui", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(ProfilActivity.this, "Gagal memperbarui kata sandi", Toast.LENGTH_SHORT).show();
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(ProfilActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
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

    interface ProgressCallback {
        void onProgressChecked(boolean isCompleted);
    }
}