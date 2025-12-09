package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import edu.uph.m23si1.homiguard.adapter.HistoryAdapter;
import edu.uph.m23si1.homiguard.model.HistoryModel;

public class LockActivity extends AppCompatActivity {

    Switch switchLock;
    TextView tvStatus;
    Toolbar toolbar;
    DatabaseReference lockRef, historyRef;

    RecyclerView recyclerHistory;
    HistoryAdapter historyAdapter;
    List<HistoryModel> historyList = new ArrayList<>();

    private boolean isUpdating = false; // 🔥 Supaya tidak trigger history 2x

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        switchLock = findViewById(R.id.switchLock);
        tvStatus = findViewById(R.id.tvStatus);
        recyclerHistory = findViewById(R.id.recyclerHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList);
        recyclerHistory.setAdapter(historyAdapter);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        FirebaseDatabase db = FirebaseDatabase.getInstance();

        // Database reference
        lockRef = db.getReference("HomiGuard/Device/Lock");
        historyRef = db.getReference("HomiGuard/History/Lock");

        // **Default locked saat activity dibuka**
        lockRef.setValue(true);

        loadHistory();

        // 🔹 Listener realtime Firebase
        lockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isLocked = snapshot.getValue(Boolean.class);
                if (isLocked == null) return;

                isUpdating = true;  // ⛔ supaya tidak trigger onCheckedChange

                switchLock.setChecked(isLocked);
                tvStatus.setText(isLocked ? "Door Unlocked 🔓" : "Door Locked 🔒");

                isUpdating = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("LockActivity", "Failed to read lock status.", error.toException());
            }
        });

        // 🔹 Listener saat user toggle manual
        switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isUpdating) return;

            // ❗ User hanya boleh UNLOCK (switch ON = unlock)
            if (isChecked) {
                lockRef.setValue(false); // false = unlocked
                tvStatus.setText("Door Unlocked 🔓");
                saveToHistory(false);
            } else {
                // ❌ Jangan biarkan user lock manual
                Toast.makeText(this, "Door will auto-lock in a few seconds", Toast.LENGTH_SHORT).show();

                // Kembalikan switch ke posisi locked
                isUpdating = true;
                switchLock.setChecked(true);
                isUpdating = false;
            }
        });

    }

    // 🔹 Save history
    private void saveToHistory(boolean isLocked) {

        long timestamp = System.currentTimeMillis();
        String value = isLocked ? "Locked" : "Unlocked";

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(timestamp));

        DatabaseReference newEntry = historyRef.push();

        newEntry.child("device").setValue("Lock");
        newEntry.child("value").setValue(value);
        newEntry.child("date").setValue(date);
        newEntry.child("timestamp").setValue(timestamp)
                .addOnSuccessListener(a -> Log.d("LockActivity", "History saved"))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save history", Toast.LENGTH_SHORT).show()
                );
    }

    // 🔹 Load history
    private void loadHistory() {
        historyRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                LinkedHashMap<String, ArrayList<HistoryModel>> grouped = new LinkedHashMap<>();
                historyList.clear();

                Calendar calNow = Calendar.getInstance();
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(calNow.getTime());
                String yesterdayStr = sdf.format(calYesterday.getTime());

                // ✅ AMBIL NAMA DEVICE DARI PATH FIREBASE (PASTI "Lock")
                String pathDevice = historyRef.getKey(); // = "Lock"

                for (DataSnapshot ds : snapshot.getChildren()) {

                    HistoryModel item = ds.getValue(HistoryModel.class);
                    if (item == null) continue;

                    // ✅ FALLBACK DEVICE (ANTI UNKNOWN)
                    if (item.getDevice() == null || item.getDevice().isEmpty()) {
                        item.setDevice(pathDevice); // otomatis jadi "Lock"
                    }

                    // ✅ FILTER DATA RUSAK
                    if (item.getTimestamp() == 0 || item.getValue() == null) continue;

                    String itemDate = sdf.format(new Date(item.getTimestamp()));

                    if (!grouped.containsKey(itemDate)) {
                        grouped.put(itemDate, new ArrayList<>());
                    }

                    grouped.get(itemDate).add(item);
                }

                ArrayList<String> dates = new ArrayList<>(grouped.keySet());
                Collections.reverse(dates);

                for (String date : dates) {

                    String header;
                    if (date.equals(todayStr)) {
                        header = "Today";
                    } else if (date.equals(yesterdayStr)) {
                        header = "Yesterday";
                    } else {
                        header = date;
                    }

                    historyList.add(new HistoryModel(header));

                    ArrayList<HistoryModel> items = grouped.get(date);
                    if (items != null) {
                        Collections.reverse(items);
                        historyList.addAll(items);
                    }
                }

                historyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LockActivity", "Failed to load history", error.toException());
            }
        });
    }
}
