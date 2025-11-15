package edu.uph.m23si1.homiguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.uph.m23si1.homiguard.model.ScheduleItem;

public class ScheduleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private FirebaseFirestore db;
    private String pageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        recyclerView = findViewById(R.id.recyclerViewSchedules);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        pageType = getIntent().getStringExtra("pageType");
        if (pageType == null) pageType = "Laundry"; // default fallback

        loadSchedules();

        Button btnBack = findViewById(R.id.btnBottomBack);
        btnBack.setOnClickListener(v -> {
            finish(); // balik ke activity sebelumnya, yaitu LaundryActivity
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedules(); // otomatis refresh
    }

    private void loadSchedules() {
        db.collection("schedules")
                .whereEqualTo("pageType", pageType)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ScheduleItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ScheduleItem item = new ScheduleItem(
                                doc.getId(),
                                doc.getString("pageType"),
                                doc.getString("date"),
                                doc.getString("onTime"),
                                doc.getString("offTime"),
                                doc.getBoolean("isActive") != null && doc.getBoolean("isActive")
                        );
                        items.add(item);
                    }

                    adapter = new ScheduleAdapter(items, db);
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
