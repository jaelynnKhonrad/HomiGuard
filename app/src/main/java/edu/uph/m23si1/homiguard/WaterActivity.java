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

                String currentHeader = "";
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Long ts = parseLong(data.child("timestamp").getValue());
                        if (ts == null) continue;

                        // convert to ms
                        if (ts < 100000000000L) ts *= 1000;

                        String date = sdf.format(new Date(ts));

                        // add date header
                        if (!date.equals(currentHeader)) {
                            currentHeader = date;
                            list.add(new HistoryModel(currentHeader));
                        }

                        Double levelCm = parseDouble(data.child("levelCm").getValue());
                        Integer percent = parseInt(data.child("percent").getValue());

                        String finalVal;
                        if (percent != null) {
                            finalVal = percent + " %";
                        } else if (levelCm != null) {
                            finalVal = String.format(Locale.getDefault(), "%.2f cm", levelCm);
                        } else {
                            finalVal = "-";
                        }

                        list.add(new HistoryModel(
                                "Water Level",
                                finalVal,
                                percent,
                                levelCm != null ? levelCm.intValue() : null,
                                ts
                        ));

                    } catch (Exception e) {
                        Log.e(TAG, "History parsing error", e);
                    }
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