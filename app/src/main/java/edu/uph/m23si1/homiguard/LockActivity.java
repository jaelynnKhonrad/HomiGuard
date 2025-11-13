package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LockActivity extends AppCompatActivity {

    private Switch switchLock;
    private TextView tvStatus;
    private DatabaseReference lockRef, historyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        switchLock = findViewById(R.id.switchLock);
        tvStatus = findViewById(R.id.tvStatus);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        lockRef = database.getReference("lock/status");
        historyRef = database.getReference("history");

        // 🔹 Baca status realtime
        lockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isLocked = snapshot.getValue(Boolean.class);
                if (isLocked != null) {
                    switchLock.setChecked(isLocked);
                    tvStatus.setText(isLocked ? "Door Locked 🔒" : "Door Unlocked 🔓");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("LockActivity", "Failed to read lock status.", error.toException());
            }
        });

        // 🔹 Ketika toggle ditekan
        switchLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                lockRef.setValue(isChecked);
                tvStatus.setText(isChecked ? "Door Locked 🔒" : "Door Unlocked 🔓");

                // Simpan ke History
                saveToHistory(isChecked);
            }
        });
    }

    // 🔹 Simpan riwayat ke Realtime Database
    private void saveToHistory(boolean isLocked) {
        String status = isLocked ? "Locked" : "Unlocked";
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        DatabaseReference todayRef = historyRef.child(date).push();
        todayRef.child("status").setValue(status);
        todayRef.child("time").setValue(time)
                .addOnSuccessListener(aVoid -> Log.d("LockActivity", "History updated: " + status))
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal simpan riwayat", Toast.LENGTH_SHORT).show());
    }
}
