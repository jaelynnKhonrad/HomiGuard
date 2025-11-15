package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LaundryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_laundry2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnSchedule = findViewById(R.id.btnSchedule);
        Button btnViewSchedule = findViewById(R.id.btnViewSchedule);

        // Buka halaman buat nambah schedule
        btnSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(LaundryActivity.this, ScheduleActivity.class);
            intent.putExtra("pageType", "Laundry");
            startActivity(intent);
        });

        // Buka halaman list schedule
        btnViewSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(LaundryActivity.this, ScheduleListActivity.class);
            intent.putExtra("pageType", "Laundry");
            startActivity(intent);
        });

        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            finish();  // langsung tutup LaundryActivity
        });

    }
}
