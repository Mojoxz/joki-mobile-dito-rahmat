package com.example.lingoquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PeringkatActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String userId;
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

        // Inisialisasi Firebase dan userId
        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        if (userId == null) {
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
        getLeaderboardData(users -> {
            if (users == null || users.isEmpty()) {
                // Jika tidak ada data, set peringkat pengguna saat ini sebagai #1
                getCurrentUserData(currentUser -> {
                    if (currentUser != null) {
                        users.clear();
                        users.add(currentUser);
                        displayLeaderboard(users);
                    }
                });
            } else {
                displayLeaderboard(users);
            }
        });
    }

    private void displayLeaderboard(List<UserData> users) {
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

    private void getLeaderboardData(LeaderboardCallback callback) {
        List<UserData> users = new ArrayList<>();
        String startDate = getStartDateForFilter(currentFilter);
        String endDate = getCurrentDate();

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    String avatarUrl = userSnapshot.child("avatar_url").getValue(String.class);

                    // Hitung total XP berdasarkan periode
                    calculateXpForPeriod(uid, startDate, endDate, xp -> {
                        // Ambil level dari user_game_progress
                        getLevel(uid, level -> {
                            users.add(new UserData(uid, username, avatarUrl, xp, level));

                            // Setelah semua data dikumpulkan
                            if (users.size() == (int) dataSnapshot.getChildrenCount()) {
                                // Urutkan berdasarkan XP (descending)
                                Collections.sort(users, (u1, u2) -> u2.getXp() - u1.getXp());

                                // Ambil 10 teratas
                                List<UserData> top10 = users.subList(0, Math.min(10, users.size()));
                                callback.onDataLoaded(top10);
                            }
                        });
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PeringkatActivity", "Error loading leaderboard: " + databaseError.getMessage());
                callback.onDataLoaded(users);
            }
        });
    }

    private void calculateXpForPeriod(String uid, String startDate, String endDate, XpCallback callback) {
        mDatabase.child("xp_history").child(uid)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int totalXp = 0;
                        for (DataSnapshot xpSnapshot : dataSnapshot.getChildren()) {
                            String timestamp = xpSnapshot.child("timestamp").getValue(String.class);
                            if (timestamp != null && isWithinPeriod(timestamp, startDate, endDate)) {
                                Integer xp = xpSnapshot.child("xp").getValue(Integer.class);
                                if (xp != null) {
                                    totalXp += xp;
                                }
                            }
                        }
                        callback.onXpCalculated(totalXp);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onXpCalculated(0);
                    }
                });
    }

    private void getLevel(String uid, LevelCallback callback) {
        mDatabase.child("user_game_progress").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int maxLevel = 1;
                        for (DataSnapshot langSnapshot : dataSnapshot.getChildren()) {
                            Integer level = langSnapshot.child("current_level").getValue(Integer.class);
                            if (level != null && level > maxLevel) {
                                maxLevel = level;
                            }
                        }
                        callback.onLevelLoaded(maxLevel);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onLevelLoaded(1);
                    }
                });
    }

    private void getCurrentUserData(UserDataCallback callback) {
        String startDate = getStartDateForFilter(currentFilter);
        String endDate = getCurrentDate();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = dataSnapshot.child("username").getValue(String.class);
                String avatarUrl = dataSnapshot.child("avatar_url").getValue(String.class);

                calculateXpForPeriod(userId, startDate, endDate, xp -> {
                    getLevel(userId, level -> {
                        callback.onUserDataLoaded(new UserData(userId, username, avatarUrl, xp, level));
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onUserDataLoaded(null);
            }
        });
    }

    private String getStartDateForFilter(String filter) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        switch (filter) {
            case "Harian":
                // Hari ini
                return sdf.format(calendar.getTime());
            case "Mingguan":
                // Awal minggu (Senin)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                return sdf.format(calendar.getTime());
            case "Bulanan":
                // Awal bulan
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                return sdf.format(calendar.getTime());
            default:
                return sdf.format(calendar.getTime());
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    private boolean isWithinPeriod(String timestamp, String startDate, String endDate) {
        return timestamp.compareTo(startDate) >= 0 && timestamp.compareTo(endDate) <= 0;
    }

    private void updateTopUser(View view, UserData user, int rank) {
        TextView name = view.findViewById(getResources().getIdentifier("name" + rank, "id", getPackageName()));
        TextView xp = view.findViewById(getResources().getIdentifier("level_xp" + rank, "id", getPackageName()));
        ImageView profile = view.findViewById(getResources().getIdentifier("profile_image" + rank, "id", getPackageName()));
        TextView crown = view.findViewById(getResources().getIdentifier("crown" + rank, "id", getPackageName()));

        name.setText(user.getUsername());
        xp.setText(String.format("%.1fK XP", user.getXp() / 1000.0));
        loadProfileImage(profile, user.getAvatarUrl());
        crown.setVisibility(rank <= 3 ? View.VISIBLE : View.GONE);
    }

    private void addUserItem(UserData user, int rank) {
        ConstraintLayout itemView = new ConstraintLayout(this);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemView.setPadding(0, 0, 0, dpToPx(16));

        TextView rankText = new TextView(this);
        rankText.setId(View.generateViewId());
        rankText.setLayoutParams(new ConstraintLayout.LayoutParams(dpToPx(40), dpToPx(40)));
        rankText.setBackgroundResource(R.drawable.circle_purple);
        rankText.setText("#" + rank);
        rankText.setTextColor(getResources().getColor(android.R.color.white));
        rankText.setGravity(android.view.Gravity.CENTER);
        itemView.addView(rankText);

        ImageView profileImage = new ImageView(this);
        profileImage.setId(View.generateViewId());
        profileImage.setLayoutParams(new ConstraintLayout.LayoutParams(dpToPx(50), dpToPx(50)));
        loadProfileImage(profileImage, user.getAvatarUrl());
        itemView.addView(profileImage);

        TextView nameText = new TextView(this);
        nameText.setId(View.generateViewId());
        nameText.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        nameText.setText(user.getUsername());
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.black));
        itemView.addView(nameText);

        TextView xpText = new TextView(this);
        xpText.setId(View.generateViewId());
        xpText.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        xpText.setText(String.format("%.1fK XP", user.getXp() / 1000.0));
        xpText.setTextSize(14);
        xpText.setTextColor(getResources().getColor(android.R.color.black));
        itemView.addView(xpText);

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

        rankText.setBackgroundResource(R.color.light_purple);
        rankText.setTextColor(getResources().getColor(android.R.color.black));

        userItems4To10.addView(itemView);
    }

    private void updateUserRank(List<UserData> users) {
        int userRank = -1;
        int userXp = 0;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(userId)) {
                userRank = i + 1;
                userXp = users.get(i).getXp();
                break;
            }
        }

        if (userRank == -1) {
            getCurrentUserData(currentUser -> {
                if (currentUser != null) {
                    users.add(currentUser);
                    Collections.sort(users, (u1, u2) -> u2.getXp() - u1.getXp());
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).getId().equals(userId)) {
                            updateUserRankUI(i + 1, users.get(i).getXp(), users);
                            break;
                        }
                    }
                }
            });
        } else {
            updateUserRankUI(userRank, userXp, users);
        }
    }

    private void updateUserRankUI(int userRank, int userXp, List<UserData> users) {
        int topXp = (users.size() > 0) ? users.get(0).getXp() : 0;

        if (userRank != -1) {
            userRankText.setText("Peringkat Anda #" + userRank);
            userXpText.setText(String.format("%.1fK XP", userXp / 1000.0));

            if (userRank == 1) {
                userProgressBar.setProgress(100);
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
        mDatabase.child("users").child(userId).child("avatar_url")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String avatarUrl = dataSnapshot.getValue(String.class);
                        loadProfileImage(userProfileImage, avatarUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        loadProfileImage(userProfileImage, null);
                    }
                });
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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Callback interfaces
    interface LeaderboardCallback {
        void onDataLoaded(List<UserData> users);
    }

    interface XpCallback {
        void onXpCalculated(int xp);
    }

    interface LevelCallback {
        void onLevelLoaded(int level);
    }

    interface UserDataCallback {
        void onUserDataLoaded(UserData user);
    }

    private static class UserData {
        private String id;
        private String username;
        private String avatarUrl;
        private int xp;
        private int level;

        public UserData(String id, String username, String avatarUrl, int xp, int level) {
            this.id = id;
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.xp = xp;
            this.level = level;
        }

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
        public int getXp() { return xp; }
        public int getLevel() { return level; }
    }
}