package edu.uph.m23si1.homiguard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.uph.m23si1.homiguard.LightingActivity;
import edu.uph.m23si1.homiguard.LockActivity;
import edu.uph.m23si1.homiguard.R;
import edu.uph.m23si1.homiguard.WaterActivity;
import edu.uph.m23si1.homiguard.LaundryActivity;

public class HomeFragment extends Fragment {

    private TextView txtNama, txtTanggal, SD_Lighting, SD_Laundry, SD_Lock, SD_Water;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtNama = view.findViewById(R.id.txtNama);
        txtTanggal = view.findViewById(R.id.txtTanggal);
        SD_Lighting = view.findViewById(R.id.SD_Lighting);
        SD_Laundry = view.findViewById(R.id.SD_Laundry);
        SD_Lock = view.findViewById(R.id.SD_Lock);
        SD_Water = view.findViewById(R.id.SD_Water);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 🔹 Set tanggal hari ini
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        txtTanggal.setText(sdf.format(new Date()));

        // 🔹 Ambil data user pertama kali
        getDataUserByEmail();

        // 🔹 Tombol See Detail
        SD_Lighting.setOnClickListener(v -> startActivity(new Intent(getActivity(), LightingActivity.class)));
        SD_Laundry.setOnClickListener(v -> startActivity(new Intent(getActivity(), LaundryActivity.class)));
        SD_Lock.setOnClickListener(v -> startActivity(new Intent(getActivity(), LockActivity.class)));
        SD_Water.setOnClickListener(v -> startActivity(new Intent(getActivity(), WaterActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🔄 Refresh nama setiap kali fragment aktif lagi (misalnya setelah update di ProfileActivity)
        getDataUserByEmail();
    }

    private void getDataUserByEmail() {
        String email = null;

        if (auth.getCurrentUser() != null) {
            email = auth.getCurrentUser().getEmail();
            Log.d("HomeFragment", "Email user login: " + email);
        }

        if (email == null) {
            Toast.makeText(getActivity(), "User email not found", Toast.LENGTH_SHORT).show();
            txtNama.setText("Hi, User!");
            return;
        }

        // 🔹 Query Firestore berdasarkan email
        firestore.collection("User_HomiGuard")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String username = document.getString("username");
                            if (username == null) username = document.getString("name");
                            if (username == null) username = document.getString("fullname");
                            if (username == null) username = "User";

                            Log.d("HomeFragment", "Nama ditemukan di Firestore: " + username);
                            txtNama.setText("Hi, " + username + "!");
                            break; // cukup ambil 1 dokumen
                        }
                    } else {
                        Log.w("HomeFragment", "Tidak ada dokumen dengan email ini");
                        txtNama.setText("Hi, User!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Gagal ambil data user berdasarkan email", e);
                    txtNama.setText("Hi, User!");
                });
    }
}