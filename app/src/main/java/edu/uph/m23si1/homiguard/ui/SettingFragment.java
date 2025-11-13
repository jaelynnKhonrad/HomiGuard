package edu.uph.m23si1.homiguard.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;

import edu.uph.m23si1.homiguard.LoginActivity;
import edu.uph.m23si1.homiguard.ProfileActivity;
import edu.uph.m23si1.homiguard.R;

public class SettingFragment extends DialogFragment {

    Button btnLogout;
    LinearLayout itemProfile;
    FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        btnLogout = view.findViewById(R.id.btnLogout);
        itemProfile = view.findViewById(R.id.itemProfile);
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        // 🔹 Klik tombol Logout
        btnLogout.setOnClickListener(v -> logoutUser());

        // 🔹 Klik item Profile
        itemProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
            dismiss(); // tutup panel setting biar langsung ke Profile
        });

        return view;
    }

    private void logoutUser() {
        // 1️⃣ Hapus session "remember me"
        SharedPreferences prefs = requireActivity().getSharedPreferences("login_pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // hapus semua data session
        editor.apply();

        // 2️⃣ Logout dari Firebase
        mAuth.signOut();

        // 3️⃣ Arahkan ke halaman login
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 4️⃣ Tutup fragment dialog
        dismiss();

        Toast.makeText(getActivity(), "Berhasil Logout", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;

            // Ukuran panel 85% layar, nempel kanan
            getDialog().getWindow().setLayout(
                    (int) (screenWidth * 0.85),
                    ViewGroup.LayoutParams.MATCH_PARENT
            );

            getDialog().getWindow().setGravity(Gravity.END); // nempel kanan
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setWindowAnimations(R.style.RightPanelAnimation);
        }
    }

    @Override
    public int getTheme() {
        return R.style.DialogTheme_Transparent;
    }
}
