package com.example.lingoquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PeringkatActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private int userId;
    private TabLayout tabLayout;
    private LinearLayout userItems4To10;
    private TextView userRankText, userXpText;
    private ProgressBar userProgressBar;
    private ImageView userProfileImage;
    private BottomNavigationView bottomNavigationView;
    private String currentFilter = "Harian"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_peringkat);

        // Inisialisasi DatabaseHelper dan userId
        db = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Inisialisasi UI components
        tabLayout = findViewById(R.id.tab_layout);
        userItems4To10 = findViewById(R.id.user_items_4_10);
        userRankText = findViewById(R.id.user_rank_text);
        userXpText = findViewById(R.id.user_xp_text);
        userProgressBar = findViewById(R.id.user_progress_bar);
        userProfileImage = findViewById(R.id.user_profile_image);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Muat foto profil pengguna saat ini
        loadCurrentUserProfileImage();

        // Atur BottomNavigationView
        bottomNavigationView.setSelectedItemId(R.id.nav_peringkat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;
            switch (navItem) {
                case NAV_HOME:
                    startActivity(new Intent(PeringkatActivity.this, MainActivity.class));
                    return true;
                case NAV_BELAJAR:
                    startActivity(new Intent(PeringkatActivity.this, BelajarActivity.class));
                    return true;
                case NAV_TANTANGAN:
                    startActivity(new Intent(PeringkatActivity.this, TantanganActivity.class));
                    return true;
                case NAV_PERINGKAT:
                    return true;
                case NAV_PROFIL:
                    startActivity(new Intent(PeringkatActivity.this, ProfilActivity.class));
                    return true;
                default:
                    return false;
            }
        });

        // Atur TabLayout untuk filter Harian, Mingguan, Bulanan
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getText().toString();
                loadLeaderboard();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Muat leaderboard awal
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        // Kosongkan daftar pengguna 4-10 sebelum memuat ulang
        userItems4To10.removeAllViews();

        // Ambil data leaderboard
        List<UserData> users = getLeaderboardData();
        if (users == null || users.isEmpty()) {
            // Jika tidak ada data, set peringkat pengguna saat ini sebagai #1
            UserData currentUser = getCurrentUserData();
            if (currentUser != null) {
                users = new ArrayList<>();
                users.add(currentUser);
            }
        }

        // Tampilkan Top 3 pengguna
        if (users.size() > 0) updateTopUser(findViewById(R.id.user_item_1), users.get(0), 1);
        if (users.size() > 1) updateTopUser(findViewById(R.id.user_item_2), users.get(1), 2);
        if (users.size() > 2) updateTopUser(findViewById(R.id.user_item_3), users.get(2), 3);

        // Tampilkan pengguna peringkat 4-10 secara dinamis
        for (int i = 3; i < Math.min(10, users.size()); i++) {
            addUserItem(users.get(i), i + 1);
        }

        // Perbarui peringkat pengguna saat ini
        updateUserRank(users);
    }

    private List<UserData> getLeaderboardData() {
        List<UserData> users = new ArrayList<>();
        Cursor cursor = db.getXpByPeriod(currentFilter, 10); // Ambil 10 pengguna teratas

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
            String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow("avatar_url"));
            int xp = cursor.isNull(cursor.getColumnIndexOrThrow("total_xp")) ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow("total_xp"));
            // Ambil level dari user_game_progress (default ke 1 jika tidak ada)
            int level = 1;
            Cursor levelCursor = db.getUserGameProgress(id, 1); // Asumsi language_id = 1 untuk sederhana
            if (levelCursor.moveToFirst()) {
                level = levelCursor.getInt(levelCursor.getColumnIndexOrThrow("current_level"));
            }
            levelCursor.close();
            users.add(new UserData(id, username, avatarUrl, xp, level));
        }
        cursor.close();

        // Urutkan berdasarkan XP (descending)
        Collections.sort(users, new Comparator<UserData>() {
            @Override
            public int compare(UserData u1, UserData u2) {
                return u2.getXp() - u1.getXp();
            }
        });

        return users;
    }

    private UserData getCurrentUserData() {
        Cursor cursor = db.getXpByPeriod(currentFilter, 1); // Ambil data pengguna saat ini
        UserData currentUser = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
            String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow("avatar_url"));
            int xp = cursor.isNull(cursor.getColumnIndexOrThrow("total_xp")) ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow("total_xp"));
            int level = 1;
            Cursor levelCursor = db.getUserGameProgress(id, 1);
            if (levelCursor.moveToFirst()) {
                level = levelCursor.getInt(levelCursor.getColumnIndexOrThrow("current_level"));
            }
            levelCursor.close();
            currentUser = new UserData(id, username, avatarUrl, xp, level);
        }
        cursor.close();
        return currentUser;
    }

    private void updateTopUser(View view, UserData user, int rank) {
        TextView name = view.findViewById(getResources().getIdentifier("name" + rank, "id", getPackageName()));
        TextView xp = view.findViewById(getResources().getIdentifier("level_xp" + rank, "id", getPackageName()));
        ImageView profile = view.findViewById(getResources().getIdentifier("profile_image" + rank, "id", getPackageName()));
        TextView crown = view.findViewById(getResources().getIdentifier("crown" + rank, "id", getPackageName()));

        name.setText(user.getUsername());
        xp.setText(String.format("%.1fK XP", user.getXp() / 1000.0));
        loadProfileImage(profile, user.getAvatarUrl());
        crown.setVisibility(rank <= 3 ? View.VISIBLE : View.GONE); // Tampilkan tanda *Winner* untuk Top 3
    }

    private void addUserItem(UserData user, int rank) {
        ConstraintLayout itemView = new ConstraintLayout(this);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemView.setPadding(0, 0, 0, dpToPx(16));

        // Tambahkan TextView untuk peringkat
        TextView rankText = new TextView(this);
        rankText.setId(View.generateViewId());
        rankText.setLayoutParams(new ConstraintLayout.LayoutParams(dpToPx(40), dpToPx(40)));
        rankText.setBackgroundResource(R.drawable.circle_purple);
        rankText.setText("#" + rank);
        rankText.setTextColor(getResources().getColor(android.R.color.white));
        rankText.setGravity(android.view.Gravity.CENTER);
        itemView.addView(rankText);

        // Tambahkan ImageView untuk foto profil
        ImageView profileImage = new ImageView(this);
        profileImage.setId(View.generateViewId());
        profileImage.setLayoutParams(new ConstraintLayout.LayoutParams(dpToPx(50), dpToPx(50)));
        loadProfileImage(profileImage, user.getAvatarUrl());
        itemView.addView(profileImage);

        // Tambahkan TextView untuk nama pengguna
        TextView nameText = new TextView(this);
        nameText.setId(View.generateViewId());
        nameText.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        nameText.setText(user.getUsername());
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.black));
        itemView.addView(nameText);

        // Tambahkan TextView untuk XP
        TextView xpText = new TextView(this);
        xpText.setId(View.generateViewId());
        xpText.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        xpText.setText(String.format("%.1fK XP", user.getXp() / 1000.0));
        xpText.setTextSize(14);
        xpText.setTextColor(getResources().getColor(android.R.color.black));
        itemView.addView(xpText);

        // Atur constraint untuk elemen-elemen
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(itemView);
        constraintSet.connect(rankText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(rankText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(profileImage.getId(), ConstraintSet.START, rankText.getId(), ConstraintSet.END, dpToPx(8));
        constraintSet.connect(profileImage.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(nameText.getId(), ConstraintSet.START, profileImage.getId(), ConstraintSet.END, dpToPx(8));
        constraintSet.connect(nameText.getId(), ConstraintSet.TOP, profileImage.getId(), ConstraintSet.TOP);
        constraintSet.connect(xpText.getId(), ConstraintSet.START, profileImage.getId(), ConstraintSet.END, dpToPx(8));
        constraintSet.connect(xpText.getId(), ConstraintSet.TOP, nameText.getId(), ConstraintSet.BOTTOM);
        constraintSet.applyTo(itemView);

        // Ubah warna latar belakang peringkat 4-10
        rankText.setBackgroundResource(R.color.light_purple); // #e6e6fa
        rankText.setTextColor(getResources().getColor(android.R.color.black));

        userItems4To10.addView(itemView);
    }

    private void updateUserRank(List<UserData> users) {
        int userRank = -1;
        int userXp = 0;

        // Cari peringkat pengguna saat ini dalam daftar
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == userId) {
                userRank = i + 1;
                userXp = users.get(i).getXp();
                break;
            }
        }

        // Jika pengguna tidak ada dalam daftar top 10, tambahkan dari getCurrentUserData
        if (userRank == -1) {
            UserData currentUser = getCurrentUserData();
            if (currentUser != null) {
                users.add(currentUser);
                Collections.sort(users, new Comparator<UserData>() {
                    @Override
                    public int compare(UserData u1, UserData u2) {
                        return u2.getXp() - u1.getXp();
                    }
                });
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).getId() == userId) {
                        userRank = i + 1;
                        userXp = users.get(i).getXp();
                        break;
                    }
                }
            }
        }

        // Ambil XP pengguna Top 1 sebagai target
        int topXp = (users.size() > 0) ? users.get(0).getXp() : 0;

        // Update UI
        if (userRank != -1) {
            userRankText.setText("Peringkat Anda #" + userRank);
            userXpText.setText(String.format("%.1fK XP", userXp / 1000.0));

            // Hitung progres untuk menjadi Top 1
            if (userRank == 1) {
                userProgressBar.setProgress(100); // Full jika peringkat 1
            } else if (topXp > 0 && userXp < topXp) {
                float progress = (float) userXp / topXp * 100;
                if (progress < 0) progress = 0;
                if (progress > 100) progress = 100;
                userProgressBar.setProgress((int) progress);
            } else {
                userProgressBar.setProgress(0);
            }
        } else {
            userRankText.setText("Peringkat Anda Tidak Ditemukan");
            userXpText.setText("0.0K XP");
            userProgressBar.setProgress(0);
        }
    }

    private void loadCurrentUserProfileImage() {
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT avatar_url FROM users WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        String avatarUrl = null;
        if (cursor.moveToFirst()) {
            avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow("avatar_url"));
        }
        cursor.close();
        loadProfileImage(userProfileImage, avatarUrl);
    }

    private void loadProfileImage(ImageView imageView, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                File file = new File(avatarUrl);
                if (file.exists()) {
                    imageView.setImageURI(Uri.fromFile(file));
                } else {
                    imageView.setImageResource(R.drawable.default_avatar);
                }
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.default_avatar);
                Log.e("PeringkatActivity", "Error loading image: " + e.getMessage());
            }
        } else {
            imageView.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class UserData {
        private int id;
        private String username;
        private String avatarUrl;
        private int xp;
        private int level;

        public UserData(int id, String username, String avatarUrl, int xp, int level) {
            this.id = id;
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.xp = xp;
            this.level = level;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
        public int getXp() { return xp; }
        public int getLevel() { return level; }
    }
}