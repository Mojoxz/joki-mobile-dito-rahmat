package com.example.lingoquest;

public enum NavigationItem {
    NAV_HOME,
    NAV_BELAJAR,
    NAV_TANTANGAN,
    NAV_PERINGKAT,
    NAV_PROFIL;

    public static NavigationItem fromItemId(int itemId) {
        if (itemId == R.id.nav_home) {
            return NAV_HOME;
        } else if (itemId == R.id.nav_belajar) {
            return NAV_BELAJAR;
        } else if (itemId == R.id.nav_tantangan) {
            return NAV_TANTANGAN;
        } else if (itemId == R.id.nav_peringkat) {
            return NAV_PERINGKAT;
        } else if (itemId == R.id.nav_profile) {
            return NAV_PROFIL;
        }
        return null;
    }
}