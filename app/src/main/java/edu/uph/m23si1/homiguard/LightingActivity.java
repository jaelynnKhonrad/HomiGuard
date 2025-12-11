package edu.uph.m23si1.homiguard;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.uph.m23si1.homiguard.adapter.HistoryAdapter;
import edu.uph.m23si1.homiguard.adapter.ScheduleAdapter;
import edu.uph.m23si1.homiguard.model.HistoryModel;
import edu.uph.m23si1.homiguard.model.ScheduleItem;

public class LightingActivity extends AppCompatActivity {
    BottomSheetBehavior bottomSheetBehavior;
    FrameLayout dimArea;
    LinearLayout scheduleListContainer, addScheduleContainer, layoutDateSelector;
    GridLayout gridRooms;
    RecyclerView rvSchedule, recyclerHistory;
    ScheduleAdapter scheduleAdapter;
    HistoryAdapter historyAdapter;
    ArrayList<ScheduleItem> scheduleList;
    ArrayList<HistoryModel> historyList;
    Button btnSchedule, btnAddSchedule, btnBackToList, btnConfirmAddSchedule;
    EditText etOnTime, etOffTime, etDate;
    Toolbar toolbar;
    Switch switchBedroom, switchLiving, switchKitchen, switchBathroom;
    RadioButton rbOnce, rbEveryday;
    View dateSpacer, schedulespacer;
    Spinner spinnerRoom;
    TextView tvStatusBedroom, tvStatusBathroom, tvStatusLiving, tvStatusKitchen, btnRoom, btnHistory;
    private String selectedDate = "";
    private String selectedRoom = "";
    private boolean isUpdating = false;

    // Firebase
    DatabaseReference lightingRoot;
    DatabaseReference historyRef;
    DatabaseReference scheduleRef;

    // Scheduler checker
    private final Handler schedulerHandler = new Handler(Looper.getMainLooper());
    private final long SCHEDULER_INTERVAL_MS = 30 * 1000L; // 30 seconds
    // prevents re-triggering same schedule many times in same minute
    private final Map<String, Long> lastExecuted = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lighting);

        // Firebase path
        lightingRoot = FirebaseDatabase.getInstance().getReference("HomiGuard/Device/Lighting");
        historyRef = FirebaseDatabase.getInstance().getReference("HomiGuard/History/Lighting");
        scheduleRef = FirebaseDatabase.getInstance().getReference("HomiGuard/Schedule/Lighting");

        // Bind Views
        dimArea = findViewById(R.id.dimArea);
        scheduleListContainer = findViewById(R.id.scheduleListContainer);
        addScheduleContainer = findViewById(R.id.addScheduleContainer);
        layoutDateSelector = findViewById(R.id.layoutDateSelector);

        rvSchedule = findViewById(R.id.rvSchedule);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        scheduleList = new ArrayList<>();
        historyList = new ArrayList<>();
        scheduleAdapter = new ScheduleAdapter(scheduleList, lightingRoot, scheduleRef);
        historyAdapter = new HistoryAdapter(historyList);
        rvSchedule.setAdapter(scheduleAdapter);
        recyclerHistory.setAdapter(historyAdapter);

        btnSchedule = findViewById(R.id.btnSchedule);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnBackToList = findViewById(R.id.btnBackToList);
        btnConfirmAddSchedule = findViewById(R.id.btnConfirmAddSchedule);
        etDate = findViewById(R.id.etDate);
        etOnTime = findViewById(R.id.etOnTime);
        etOffTime = findViewById(R.id.etOffTime);
        toolbar = findViewById(R.id.toolbar);
        rbEveryday = findViewById(R.id.rbEveryday);
        rbOnce = findViewById(R.id.rbOnce);
        dateSpacer = findViewById(R.id.dateSpacer);
        schedulespacer = findViewById(R.id.schedulespacer);
        spinnerRoom = findViewById(R.id.spinnerRoom);
        switchBedroom = findViewById(R.id.switchBedroom);
        switchLiving = findViewById(R.id.switchLiving);
        switchKitchen = findViewById(R.id.switchKitchen);
        switchBathroom = findViewById(R.id.switchBathroom);
        layoutDateSelector.setVisibility(VISIBLE);
        dateSpacer.setVisibility(GONE);
        tvStatusBedroom = findViewById(R.id.tvStatusBedroom);
        tvStatusBathroom = findViewById(R.id.tvStatusBathroom);
        tvStatusKitchen = findViewById(R.id.tvStatusKitchen);
        tvStatusLiving = findViewById(R.id.tvStatusLiving);
        btnRoom = findViewById(R.id.btnRoom);
        btnHistory = findViewById(R.id.btnHistory);
        gridRooms = findViewById(R.id.gridRooms);

        setupSpinner();
        setupToolbarAndTabs();
        setupSwitchControls();
        setupBottomSheetControls();

        etDate.setOnClickListener(v -> pickDate());

        etOnTime.setOnClickListener(v -> showTimePicker((hour, minute) ->
                etOnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        ));

        etOffTime.setOnClickListener(v -> showTimePicker((hour, minute) ->
                etOffTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        ));

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

        loadScheduleData();
        loadHistory();
        attachAllRoomListeners(); // attaches Firebase listeners to switches (keeps UI in sync)

        // Start simple scheduler checker (runs while app process alive)
        startSchedulerChecker();
    }

    // ------------------------
    // setup helpers
    // ------------------------
    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.room_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);

        spinnerRoom.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedRoom = parent.getItemAtPosition(position).toString();
                // we keep loadRoomStatus general; the UI listeners already update switches
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupToolbarAndTabs() {
        // Toolbar back
        toolbar.setNavigationOnClickListener(v -> {
            if (addScheduleContainer.getVisibility() == VISIBLE) {
                addScheduleContainer.setVisibility(GONE);
                scheduleListContainer.setVisibility(VISIBLE);
                toolbar.setTitle("Schedule");
            } else finish();
        });

        btnRoom.setOnClickListener(v -> {
            gridRooms.setVisibility(View.VISIBLE);
            schedulespacer.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
            btnRoom.setBackgroundResource(R.drawable.tab_active);
            btnHistory.setBackgroundResource(R.drawable.tab_inactive);
        });

        btnHistory.setOnClickListener(v -> {
            gridRooms.setVisibility(View.GONE);
            schedulespacer.setVisibility(View.GONE);
            recyclerHistory.setVisibility(View.VISIBLE);
            btnRoom.setBackgroundResource(R.drawable.tab_inactive);
            btnHistory.setBackgroundResource(R.drawable.tab_active);
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
    }

    private void setupSwitchControls() {
        // When user toggles switch -> update Firebase & history.
        // Use isUpdating flag to avoid responding to our own programmatic setChecked() changes.
        switchBedroom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdating) return;
            lightingRoot.child("Bedroom").setValue(isChecked);
            saveToHistory("Bedroom", isChecked);
            tvStatusBedroom.setText(isChecked ? "ON 💡" : "OFF");
        });
        switchKitchen.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isUpdating) return;
            lightingRoot.child("Kitchen").setValue(isChecked);
            saveToHistory("Kitchen", isChecked);
            tvStatusKitchen.setText(isChecked ? "ON 💡" : "OFF");
        });
        switchLiving.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isUpdating) return;
            lightingRoot.child("Livingroom").setValue(isChecked);
            saveToHistory("Livingroom", isChecked);
            tvStatusLiving.setText(isChecked ? "ON 💡" : "OFF");
        });
        switchBathroom.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isUpdating) return;
            lightingRoot.child("Bathroom").setValue(isChecked);
            saveToHistory("Bathroom", isChecked);
            tvStatusBathroom.setText(isChecked ? "ON 💡" : "OFF");
        });
    }

    private void setupBottomSheetControls() {
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
            recyclerHistory.setZ(-10f);
        });

        dimArea.setOnClickListener(v -> closeBottomSheet());

        btnAddSchedule.setOnClickListener(v -> {
            scheduleListContainer.setVisibility(GONE);
            addScheduleContainer.setVisibility(VISIBLE);
            toolbar.setTitle("Add Schedule");
        });
    }

    // ------------------------
    // Firebase listeners & UI sync
    // ------------------------
    private void attachAllRoomListeners() {
        // Generic attach to avoid repeating code
        attachSwitchListener("Bedroom", switchBedroom, tvStatusBedroom);
        attachSwitchListener("Kitchen", switchKitchen, tvStatusKitchen);
        attachSwitchListener("Livingroom", switchLiving, tvStatusLiving);
        attachSwitchListener("Bathroom", switchBathroom, tvStatusBathroom);
    }

    private void attachSwitchListener(String room, Switch switchView, TextView statusView) {
        lightingRoot.child(room).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean state = snapshot.getValue(Boolean.class);
                isUpdating = true;
                boolean checked = state != null && state;
                switchView.setChecked(checked);
                statusView.setText(checked ? "ON 💡" : "OFF");
                isUpdating = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ignore for now
            }
        });
    }

    // ------------------------
    // Schedule handling (loader + executor)
    // ------------------------
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
                Toast.makeText(LightingActivity.this, "Failed to load schedule", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This method checks schedules now and triggers Firebase updates if needed.
    // Runs periodically while app is alive (see startSchedulerChecker()).
    private void checkAndRunSchedulesNow() {
        scheduleRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;

            Calendar now = Calendar.getInstance();
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            String currentTime = sdfTime.format(now.getTime()); // "HH:mm"
            String currentDate = sdfDate.format(now.getTime()); // "yyyy-MM-dd"

            for (DataSnapshot ds : task.getResult().getChildren()) {
                ScheduleItem item = ds.getValue(ScheduleItem.class);
                if (item == null || !item.isActive()) continue;

                boolean isToday = "Everyday".equals(item.getDate()) || currentDate.equals(item.getDate());

                if (!isToday) continue;

                // try ON
                if (item.getOnTime() != null && item.getOnTime().equals(currentTime)) {
                    String execKey = item.getKey() + "_ON";
                    if (shouldExecute(execKey, now)) {
                        // set Firebase value to true
                        lightingRoot.child(item.getRoom()).setValue(true);
                        saveToHistory(item.getRoom(), true);
                        markExecuted(execKey, now);
                    }
                }
                // try OFF
                if (item.getOffTime() != null && item.getOffTime().equals(currentTime)) {
                    String execKey = item.getKey() + "_OFF";
                    if (shouldExecute(execKey, now)) {
                        lightingRoot.child(item.getRoom()).setValue(false);
                        saveToHistory(item.getRoom(), false);
                        markExecuted(execKey, now);
                    }
                }
            }
        });
    }

    private boolean shouldExecute(String execKey, Calendar now) {
        // prevent re-trigger within same minute
        long minuteId = now.getTimeInMillis() / (60 * 1000L);
        Long last = lastExecuted.get(execKey);
        return last == null || last != minuteId;
    }

    private void markExecuted(String execKey, Calendar now) {
        long minuteId = now.getTimeInMillis() / (60 * 1000L);
        lastExecuted.put(execKey, minuteId);
    }

    private final Runnable schedulerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                checkAndRunSchedulesNow();
            } finally {
                // schedule next run
                schedulerHandler.postDelayed(this, SCHEDULER_INTERVAL_MS);
            }
        }
    };

    private void startSchedulerChecker() {
        // start immediately
        schedulerHandler.post(schedulerRunnable);
    }

    private void stopSchedulerChecker() {
        schedulerHandler.removeCallbacks(schedulerRunnable);
    }

    // ------------------------
    // History, timepickers, datepicker, add schedule
    // ------------------------
    private void saveToHistory(String room, boolean isOn) {
        long timestamp = System.currentTimeMillis();

        HistoryModel item = new HistoryModel(
                room,                    // device = nama ruangan
                isOn ? "On" : "Off",     // value = On/Off
                0,                       // percent (nggak dipakai)
                0,                       // levelCm (nggak dipakai)
                timestamp
        );
        historyRef.push().setValue(item);
    }

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

                            Object tsObj = ds.child("timestamp").getValue();
                            long ts = 0;

                            if (tsObj instanceof Long) {
                                ts = (Long) tsObj;
                            } else if (tsObj instanceof String) {
                                try {
                                    ts = Long.parseLong((String) tsObj);
                                } catch (Exception ignored) {}
                            }
                            // kalau detik (10 digit), ubah ke milidetik
                            if (ts < 100000000000L) {   // kurang dari 13 digit
                                ts = ts * 1000;
                            }
                            // set timestamp yg udah bener
                            item.setTimestamp(ts);

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
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void addNewSchedule() {
        String onTime = etOnTime.getText().toString();
        String offTime = etOffTime.getText().toString();

        if ((selectedDate == null || selectedDate.isEmpty()) || onTime.isEmpty() || offTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedRoomLocal = spinnerRoom.getSelectedItem().toString();
        String key = selectedRoomLocal + "_" + selectedDate.replace("-", "") + "_" + onTime + "_" + offTime;
        ScheduleItem item = new ScheduleItem(
                key,
                "Lighting",
                selectedRoomLocal,
                selectedDate,
                onTime,
                offTime,
                true
        );
        scheduleRef.child(key).setValue(item)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Schedule added", Toast.LENGTH_SHORT).show();
                        etOnTime.setText("");
                        etOffTime.setText("");
                        addScheduleContainer.setVisibility(GONE);
                        scheduleListContainer.setVisibility(VISIBLE);
                        toolbar.setTitle("Schedule");
                    } else {
                        Toast.makeText(this, "Failed to add schedule", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTimePicker(TimeListener listener) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, h, m) -> listener.onTimeSelected(h, m), hour, minute, true
        );
        dialog.show();
    }

    private void pickDate() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String formattedDate = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault()).format(cal.getTime());
            etDate.setText(formattedDate);
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

    // ------------------------
    // lifecycle
    // ------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSchedulerChecker();
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

    private void closeBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        dimArea.setVisibility(GONE);
        recyclerHistory.setZ(0f);
    }
}




