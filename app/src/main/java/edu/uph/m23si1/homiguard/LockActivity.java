package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import android.os.Handler;
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

    private Switch switchLock;
    private TextView tvStatus;
    private Toolbar toolbar;

    private DatabaseReference lockRef, historyRef;

    private RecyclerView recyclerHistory;
    private HistoryAdapter historyAdapter;
    private List<HistoryModel> historyList = new ArrayList<>();

    private boolean isUpdating = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        switchLock = findViewById(R.id.switchLock);
        tvStatus = findViewById(R.id.tvStatus);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList);
        recyclerHistory.setAdapter(historyAdapter);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        lockRef = db.getReference("HomiGuard/Device/Lock");
        historyRef = db.getReference("HomiGuard/History/Lock");

        loadHistory();

        // Listen real-time status
        lockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isLocked = snapshot.getValue(Boolean.class);
                if (isLocked == null) return;

                isUpdating = true;

                switchLock.setChecked(!isLocked); // ON = unlocked
                tvStatus.setText(isLocked ? "Door Locked 🔒" : "Door Unlocked 🔓");

                isUpdating = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LockActivity", "Failed to read lock state", error.toException());
            }
        });

        // User toggle
        switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdating) return;

            if (isChecked) {
                // User unlock
                lockRef.setValue(false);
                tvStatus.setText("Door Unlocked 🔓");
                saveToHistory(false);

                // Auto-lock in 3 seconds
                handler.postDelayed(() -> {
                    lockRef.setValue(true);
                    saveToHistory(true);
                }, 3000);

            } else {
                // User tries to lock manually
                Toast.makeText(this, "Door auto-locks shortly", Toast.LENGTH_SHORT).show();

                isUpdating = true;
                switchLock.setChecked(true);
                isUpdating = false;
            }
        });
    }

    // Save history
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
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save history", Toast.LENGTH_SHORT).show());
    }

    // Load history & group by date
    private void loadHistory() {
        historyRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
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

//                        for (DataSnapshot ds : snapshot.getChildren()) {
//
//                            HistoryModel item = ds.getValue(HistoryModel.class);
//                            if (item == null) continue;
//
//                            // ✅ AMBIL TIMESTAMP AMAN (ANDROID + ARDUINO)
//                            Object tsObj = ds.child("timestamp").getValue();
//                            long ts = 0;
//
//                            if (tsObj instanceof Long) {
//                                ts = (Long) tsObj;
//                            } else if (tsObj instanceof String) {
//                                try {
//                                    ts = Long.parseLong((String) tsObj);
//                                } catch (Exception ignored) {}
//                            }
//
//                            // ✅ JIKA TIMESTAMP DARI ARDUINO (DETIK → MILIDETIK)
//                            if (ts < 100000000000L) { // masih 10 digit
//                                ts = ts * 1000;
//                            }
//
//                            item.setTimestamp(ts);
//
//                            // ✅ PAKSA DEVICE JADI "Lock" JIKA KOSONG
//                            if (item.getDevice() == null || item.getDevice().isEmpty()) {
//                                item.setDevice("Lock");
//                            }
//
//                            String itemDate = sdf.format(item.getTimestamp());
//
//                            grouped.putIfAbsent(itemDate, new ArrayList<>());
//                            grouped.get(itemDate).add(item);
//                        }

                        //new code
                        for (DataSnapshot ds : snapshot.getChildren()) {

                            // ✅ AMBIL MANUAL (biar support semua struktur)
                            HistoryModel item = new HistoryModel();

                            item.setDevice(ds.child("device").getValue(String.class));
                            item.setValue(ds.child("value").getValue(String.class));
                            item.setDate(ds.child("date").getValue(String.class));

                            // ✅ AMBIL TIMESTAMP AMAN (ANDROID + ARDUINO)
                            Object tsObj = ds.child("timestamp").getValue();
                            long ts = 0;

                            if (tsObj instanceof Long) {
                                ts = (Long) tsObj;
                            } else if (tsObj instanceof String) {
                                try {
                                    ts = Long.parseLong((String) tsObj);
                                } catch (Exception ignored) {}
                            } else if (tsObj instanceof Integer) {
                                ts = ((Integer) tsObj).longValue();
                            }

                            // ✅ JIKA TIMESTAMP DARI ARDUINO (DETIK → MILIDETIK)
                            if (ts < 100000000000L) { // masih 10 digit
                                ts = ts * 1000;
                            }

                            item.setTimestamp(ts);

                            // ✅ PAKSA DEVICE JADI "Lock" JIKA KOSONG
                            if (item.getDevice() == null || item.getDevice().isEmpty()) {
                                item.setDevice("Lock");
                            }

                            // ✅ SKIP jika data kosong
                            if (item.getValue() == null || item.getTimestamp() == 0) {
                                continue;
                            }

                            String itemDate = sdf.format(item.getTimestamp());

                            grouped.putIfAbsent(itemDate, new ArrayList<>());
                            grouped.get(itemDate).add(item);
                        }

                        List<String> dates = new ArrayList<>(grouped.keySet());
                        Collections.sort(dates, (d1, d2) -> d2.compareTo(d1));

                        for (String date : dates) {

                            String header =
                                    date.equals(todayStr) ? "Today" :
                                            date.equals(yesterdayStr) ? "Yesterday" : date;

                            historyList.add(new HistoryModel(header));

                            ArrayList<HistoryModel> items = grouped.get(date);
                            Collections.reverse(items);
                            historyList.addAll(items);
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