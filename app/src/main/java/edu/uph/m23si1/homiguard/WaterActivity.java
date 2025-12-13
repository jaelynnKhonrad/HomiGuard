package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

import edu.uph.m23si1.homiguard.adapter.HistoryAdapter;
import edu.uph.m23si1.homiguard.model.HistoryModel;

public class WaterActivity extends AppCompatActivity {

    private static final String TAG = "WaterActivity";

    Toolbar toolbar;
    TextView tvCurrentLevel;
    RecyclerView recyclerHistory;

    HistoryAdapter historyAdapter;
    ArrayList<HistoryModel> list = new ArrayList<>();

    // 🔥 PATH BARU SESUAI DATABASE
    DatabaseReference refStatus = FirebaseDatabase.getInstance()
            .getReference("HomiGuard")
            .child("Device")
            .child("Water")
            .child("status");

    DatabaseReference refHistory = FirebaseDatabase.getInstance()
            .getReference("HomiGuard")
            .child("History")
            .child("Water");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        recyclerHistory = findViewById(R.id.recyclerHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(list);
        recyclerHistory.setAdapter(historyAdapter);

        loadCurrentWaterStatus();
        loadWaterHistory();
    }

    // ============================================================
    // 🔵 LOAD CURRENT STATUS (REALTIME)
    // ============================================================

    private void loadCurrentWaterStatus() {

        DatabaseReference refStatus = FirebaseDatabase.getInstance()
                .getReference("HomiGuard")
                .child("Device")
                .child("Water");

        refStatus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    tvCurrentLevel.setText("No Data");
                    return;
                }

                Double waterValue = snapshot.getValue(Double.class);

                if (waterValue != null) {
                    tvCurrentLevel.setText(waterValue + " %");
                } else {
                    tvCurrentLevel.setText("No Data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("WATER", "Cancelled", error.toException());
            }
        });
    }






    // ============================================================
    // 🔵 LOAD HISTORY
    // ============================================================
    private void loadWaterHistory() {

        Query query = refHistory.orderByChild("timestamp").limitToLast(50);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                list.clear();

                if (!snapshot.exists()) {
                    historyAdapter.notifyDataSetChanged();
                    return;
                }

                // ================================
                // SETUP TODAY / YESTERDAY CHECK
                // ================================
                Calendar calNow = Calendar.getInstance();
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);

                SimpleDateFormat fullDateFormat =
                        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                SimpleDateFormat compareFormat =
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                String todayStr = compareFormat.format(calNow.getTime());
                String yesterdayStr = compareFormat.format(calYesterday.getTime());

                // Grouping by date
                LinkedHashMap<String, ArrayList<HistoryModel>> grouped = new LinkedHashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        // Timestamp safe parsing
                        Long ts = parseLong(data.child("timestamp").getValue());
                        if (ts == null) continue;

                        if (ts < 100000000000L) ts *= 1000; // convert seconds → ms

                        Double levelCm = parseDouble(data.child("levelCm").getValue());
                        Integer percent = parseInt(data.child("percent").getValue());

                        // determine final value shown
                        String finalVal;
                        if (percent != null) {
                            finalVal = percent + " %";
                        } else if (levelCm != null) {
                            finalVal = String.format(Locale.getDefault(), "%.2f cm", levelCm);
                        } else {
                            finalVal = "-";
                        }

                        HistoryModel item = new HistoryModel(
                                "Water Level",
                                finalVal,
                                percent,
                                levelCm != null ? levelCm.floatValue() : 0f,
                                ts
                        );

                        // Convert timestamp → date key (yyyy-MM-dd)
                        String keyDate = compareFormat.format(new Date(ts));

                        grouped.putIfAbsent(keyDate, new ArrayList<>());
                        grouped.get(keyDate).add(item);

                    } catch (Exception e) {
                        Log.e(TAG, "History parsing error", e);
                    }
                }

                // ================================
                // BUILD FINAL LIST WITH HEADERS
                // ================================
                List<String> dates = new ArrayList<>(grouped.keySet());

                // newest first
                Collections.sort(dates, (d1, d2) -> d2.compareTo(d1));

                for (String date : dates) {

                    String headerName =
                            date.equals(todayStr) ? "Today" :
                                    date.equals(yesterdayStr) ? "Yesterday" :
                                            fullDateFormat.format(compareFormat.parse(date, new java.text.ParsePosition(0)));

                    list.add(new HistoryModel(headerName));

                    ArrayList<HistoryModel> items = grouped.get(date);

                    // newest at bottom (like messaging apps)
                    Collections.reverse(items);

                    list.addAll(items);
                }

                historyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "History cancelled", error.toException());
            }
        });
    }

    // ============================================================
    // 🔵 Helper Parsing
    // ============================================================
    private Double parseDouble(Object obj) {
        try {
            if (obj instanceof Double) return (Double) obj;
            if (obj instanceof Long) return ((Long) obj).doubleValue();
            if (obj instanceof Integer) return ((Integer) obj).doubleValue();
            if (obj instanceof String) return Double.parseDouble((String) obj);
        } catch (Exception e) {}
        return null;
    }

    private Integer parseInt(Object obj) {
        try {
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof Double) return ((Double) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        } catch (Exception e) {}
        return null;
    }

    private Long parseLong(Object obj) {
        try {
            if (obj instanceof Long) return (Long) obj;
            if (obj instanceof Integer) return ((Integer) obj).longValue();
            if (obj instanceof Double) return ((Double) obj).longValue();
            if (obj instanceof String) return Long.parseLong((String) obj);
        } catch (Exception e) {}
        return null;
    }
}