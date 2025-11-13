package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etEmail, etName, etPassword;
    private Button btnSave;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;

    private String firestoreDocId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        if (user != null) {
            etEmail.setText(user.getEmail());
        }

        // tampilkan bintang password (gak bisa diubah)
        etPassword.setText("********");
        etPassword.setEnabled(false);

        // ambil nama user login
        loadCurrentUserData();

        // klik tombol save
        btnSave.setOnClickListener(v -> saveName());

        // tombol back arahkan ke home
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.putExtra("openFragment", "home"); // biar tau buka HomeFragment
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadCurrentUserData() {
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();

        firestore.collection("User_HomiGuard")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            firestoreDocId = doc.getId();

                            String username = doc.getString("username");
                            if (username == null) username = doc.getString("name");
                            if (username == null) username = doc.getString("fullname");
                            if (username == null) username = "";

                            etName.setText(username);
                            break;
                        }
                    } else {
                        String dn = user.getDisplayName();
                        etName.setText(!TextUtils.isEmpty(dn) ? dn : "");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Gagal ambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    String dn = user.getDisplayName();
                    if (!TextUtils.isEmpty(dn)) etName.setText(dn);
                });
    }

    private void saveName() {
        String newName = etName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            etName.setError("Nama tidak boleh kosong");
            etName.requestFocus();
            return;
        }

        btnSave.setEnabled(false);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateNameInFirestore(newName);
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Gagal update nama: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateNameInFirestore(String newName) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("username", newName);

        if (firestoreDocId != null) {
            firestore.collection("User_HomiGuard")
                    .document(firestoreDocId)
                    .update(updateMap)
                    .addOnSuccessListener(unused -> finishSuccess())
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Gagal update Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            String uid = user.getUid();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("email", user.getEmail());
            userMap.put("username", newName);

            firestore.collection("User_HomiGuard")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener(unused -> finishSuccess())
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Gagal simpan ke Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void finishSuccess() {
        btnSave.setEnabled(true);
        Toast.makeText(this, "Nama berhasil diperbarui", Toast.LENGTH_SHORT).show();
        finish();
    }
}
