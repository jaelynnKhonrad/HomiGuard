package edu.uph.m23si1.homiguard;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Switch;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import edu.uph.m23si1.homiguard.adapter.HistoryAdapter;
import edu.uph.m23si1.homiguard.adapter.ScheduleAdapter;
import edu.uph.m23si1.homiguard.model.HistoryModel;
import edu.uph.m23si1.homiguard.model.ScheduleItem;

public class LaundryActivity extends AppCompatActivity {

    BottomSheetBehavior bottomSheetBehavior;
    FrameLayout dimArea;
    LinearLayout scheduleListContainer, addScheduleContainer, layoutDateSelector;
    RecyclerView rvSchedule, recyclerHistoryLaundry;
    ScheduleAdapter scheduleAdapter;
    HistoryAdapter historyAdapter;
    ArrayList<ScheduleItem> scheduleList;
    ArrayList<HistoryModel> historyList;
    Button btnSchedule, btnAddSchedule, btnBackToList, btnConfirmAddSchedule;
    EditText etOnTime, etOffTime, etDate;
    Toolbar toolbar;
    Switch switchLaundry;
    RadioButton rbOnce, rbEveryday;
    View dateSpacer;
    DatabaseReference deviceRef, historyRef, scheduleRef;

    private String selectedDate = "";
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laundry);

        // =============================
        // Firebase
        // =============================
        deviceRef = FirebaseDatabase.getInstance().getReference("HomiGuard/Device/Laundry");
        historyRef = FirebaseDatabase.getInstance().getReference("HomiGuard/History/Laundry");
        scheduleRef = FirebaseDatabase.getInstance().getReference("HomiGuard/Schedule/Laundry");

        // =============================
        // Bind Views
        // =============================
        dimArea = findViewById(R.id.dimArea);
        scheduleListContainer = findViewById(R.id.scheduleListContainer);
        addScheduleContainer = findViewById(R.id.addScheduleContainer);
        layoutDateSelector = findViewById(R.id.layoutDateSelector);

        rvSchedule = findViewById(R.id.rvSchedule);
        recyclerHistoryLaundry = findViewById(R.id.recyclerHistoryLaundry);
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistoryLaundry.setLayoutManager(new LinearLayoutManager(this));

        scheduleList = new ArrayList<>();
        historyList = new ArrayList<>();

        scheduleAdapter = new ScheduleAdapter(scheduleList, deviceRef, scheduleRef);
        historyAdapter = new HistoryAdapter(historyList);

        rvSchedule.setAdapter(scheduleAdapter);
        recyclerHistoryLaundry.setAdapter(historyAdapter);

        btnSchedule = findViewById(R.id.btnSchedule);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnBackToList = findViewById(R.id.btnBackToList);
        btnConfirmAddSchedule = findViewById(R.id.btnConfirmAddSchedule);
        etDate = findViewById(R.id.etDate);
        etOnTime = findViewById(R.id.etOnTime);
        etOffTime = findViewById(R.id.etOffTime);
        toolbar = findViewById(R.id.toolbar);
        switchLaundry = findViewById(R.id.switchLaundry);
        rbEveryday = findViewById(R.id.rbEveryday);
        rbOnce = findViewById(R.id.rbOnce);
        dateSpacer = findViewById(R.id.dateSpacer);

        layoutDateSelector.setVisibility(VISIBLE);
        dateSpacer.setVisibility(GONE);

        // Toolbar Back
        toolbar.setNavigationOnClickListener(v -> {
            if (addScheduleContainer.getVisibility() == VISIBLE) {
                addScheduleContainer.setVisibility(GONE);
                scheduleListContainer.setVisibility(VISIBLE);
                toolbar.setTitle("Schedule");
            } else finish();
        });

        // State Listener (Realtime)
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean state = snapshot.getValue(Boolean.class);
                if (state == null) return;

                isUpdating = true;
                switchLaundry.setChecked(state);
                updateLaundryStatusText(state);
                isUpdating = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        switchLaundry.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdating) return;

            deviceRef.setValue(isChecked);
            updateLaundryStatusText(isChecked);
            saveToHistory(isChecked);
        });

        // Bottom Sheet
        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setFitToContents(true);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        btnSchedule.setOnClickListener(v -> {
            dimArea.setVisibility(VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            recyclerHistoryLaundry.setZ(-10f);
        });

        dimArea.setOnClickListener(v -> closeBottomSheet());

        btnAddSchedule.setOnClickListener(v -> {
            scheduleListContainer.setVisibility(GONE);
            addScheduleContainer.setVisibility(VISIBLE);
            toolbar.setTitle("Add Schedule");
        });

        rbEveryday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedDate = "Everyday";
                layoutDateSelector.setVisibility(GONE);
                dateSpacer.setVisibility(VISIBLE);
            }
        });

        rbOnce.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                dateSpacer.setVisibility(GONE);
                layoutDateSelector.setVisibility(VISIBLE);
            }
        });

        etDate.setOnClickListener(v -> {
            pickDate();
        });
        etOnTime.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                etOnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            });
        });
        etOffTime.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                etOffTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            });
        });

        btnBackToList.setOnClickListener(v -> {
            addScheduleContainer.setVisibility(GONE);
            scheduleListContainer.setVisibility(VISIBLE);
            etDate.setText("");
            etOnTime.setText("");
            etOffTime.setText("");
            rbOnce.setChecked(true);
            rbEveryday.setChecked(false);
        });

        btnConfirmAddSchedule.setOnClickListener(v -> addNewSchedule());

        // Load Initial Data
        loadScheduleData();
        loadHistory();
    }

    // Update Status Text
    private void updateLaundryStatusText(boolean isOn) {
        TextView tvStatus = findViewById(R.id.tvStatusLaundry);
        tvStatus.setText(isOn ? "It's not raining 🌤️" : "It's raining 🌧️");
    }

    // Save History
    private void saveToHistory(boolean isOn) {

        long timestamp = System.currentTimeMillis();

        HistoryModel item = new HistoryModel(
                "Laundry",
                isOn ? "On" : "Off",
                0,
                0,
                timestamp
        );

        historyRef.push().setValue(item);
    }

    // Load History
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

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            HistoryModel item = ds.getValue(HistoryModel.class);
                            if (item == null) continue;
                            String itemDate = sdf.format(item.getTimestamp());
                            grouped.putIfAbsent(itemDate, new ArrayList<>());
                            grouped.get(itemDate).add(item);
                        }

                        // --- URUTKAN TANGGAL TERBARU DI ATAS ---
                        List<String> dates = new ArrayList<>(grouped.keySet());
                        Collections.sort(dates, (d1, d2) -> d2.compareTo(d1)); // descending

                        // --- MASUKKAN KE LIST ---
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
                            Collections.reverse(items); // item terbaru dulu
                            historyList.addAll(items);
                        }

                        historyAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // Close Bottom Sheet
    private void closeBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        dimArea.setVisibility(GONE);
        recyclerHistoryLaundry.setZ(0f);
    }

    // Load Schedule
    private void loadScheduleData() {
        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scheduleList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ScheduleItem item = ds.getValue(ScheduleItem.class);
                    if (item != null) scheduleList.add(item);
                }
                scheduleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LaundryActivity.this, "Failed to load schedule", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add Schedule
    private void addNewSchedule() {

        String onTime = etOnTime.getText().toString();
        String offTime = etOffTime.getText().toString();

        if (selectedDate.isEmpty() || onTime.isEmpty() || offTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = selectedDate.replace("-", "") + "_" + onTime + "_" + offTime;

        ScheduleItem item = new ScheduleItem(
                key, "Laundry", "Laundry",
                selectedDate, onTime, offTime, true
        );

        scheduleRef.child(key).setValue(item)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        Toast.makeText(this, "Schedule added", Toast.LENGTH_SHORT).show();

                        // Clear input
                        etOnTime.setText("");
                        etOffTime.setText("");

                        // Back to list
                        addScheduleContainer.setVisibility(GONE);
                        scheduleListContainer.setVisibility(VISIBLE);
                        toolbar.setTitle("Schedule");

                    }
                });
    }

    private void showTimePicker(TimeListener listener) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, h, m) -> listener.onTimeSelected(h, m),
                hour,
                minute,
                true
        );

        dialog.show();
    }
    private void pickDate() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    // Format untuk tampilan (Mon, 20 January 2025)
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, month, dayOfMonth);

                    String formattedDate = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault())
                            .format(cal.getTime());

                    etDate.setText(formattedDate);

                    Toast.makeText(this, "Selected: " + selectedDate, Toast.LENGTH_SHORT).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    interface TimeListener {
        void onTimeSelected(int hour, int minute);
    }


    @Override
    public void onBackPressed() {
        if (addScheduleContainer.getVisibility() == VISIBLE) {
            addScheduleContainer.setVisibility(GONE);
            scheduleListContainer.setVisibility(VISIBLE);
        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            closeBottomSheet();
        } else super.onBackPressed();
    }
}