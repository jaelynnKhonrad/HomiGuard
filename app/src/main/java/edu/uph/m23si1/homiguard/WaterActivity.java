package edu.uph.m23si1.homiguard;

import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import edu.uph.m23si1.homiguard.adapter.HistoryAdapter;
import edu.uph.m23si1.homiguard.model.HistoryModel;

public class WaterActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView tvCurrentLevel;
    RecyclerView recyclerHistory;

    HistoryAdapter historyAdapter;
    ArrayList<HistoryModel> list = new ArrayList<>();

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("water");

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
        toolbar.setOnClickListener(v -> finish());

        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        recyclerHistory = findViewById(R.id.recyclerHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(list);
        recyclerHistory.setAdapter(historyAdapter);

        loadCurrentWaterStatus();
        loadWaterHistory();
    }

    private void loadCurrentWaterStatus() {
        ref.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // misal dari Arduino: { "distance_cm": 13 }
                Integer cm = snapshot.child("distance_cm").getValue(Integer.class);
                Integer percent = snapshot.child("percent").getValue(Integer.class);

                if (cm != null) {
                    tvCurrentLevel.setText(cm + " cm");
                } else if (percent != null) {
                    tvCurrentLevel.setText(percent + " %");
                } else {
                    tvCurrentLevel.setText("No Data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadWaterHistory() {
        ref.child("history").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();

                String currentHeader = "";
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    Long ts = data.child("timestamp").getValue(Long.class);
                    Integer cm = data.child("distance_cm").getValue(Integer.class);
                    Integer percent = data.child("percent").getValue(Integer.class);

                    if (ts == null) continue;

                    String date = sdf.format(new Date(ts));

                    // bikin header per tanggal
                    if (!currentHeader.equals(date)) {
                        currentHeader = date;
                        list.add(new HistoryModel(currentHeader)); // header
                    }

                    // value display
                    String finalValue;
                    if (cm != null) finalValue = cm + " cm";
                    else if (percent != null) finalValue = percent + " %";
                    else finalValue = "-";

                    HistoryModel model = new HistoryModel(
                            "Water Level",
                            "-",
                            percent,
                            cm,
                            ts
                    );

                    list.add(model);
                }

                historyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
