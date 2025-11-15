package edu.uph.m23si1.homiguard;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    private LinearLayout layoutDateSelector;
    private TextView tvMonthYear, tvOnAmPm, tvOffAmPm;
    private EditText etOnTime, etOffTime;
    private Button btnClearAll, btnConfirmSchedule;
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar currentMonth = Calendar.getInstance();

    private String pageType;
    private FirebaseFirestore db;

    private String selectedDay = "";
    private String onTime = "";
    private String offTime = "";
    private String onAmPm = "AM";
    private String offAmPm = "AM";

    private String documentId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule2);

        db = FirebaseFirestore.getInstance();

        layoutDateSelector = findViewById(R.id.layoutDateSelector);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvOnAmPm = findViewById(R.id.tvOnAmPm);
        tvOffAmPm = findViewById(R.id.tvOffAmPm);
        etOnTime = findViewById(R.id.etOnTime);
        etOffTime = findViewById(R.id.etOffTime);
        btnClearAll = findViewById(R.id.btnClearAll);
        btnConfirmSchedule = findViewById(R.id.btnConfirmSchedule);

        ImageView btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageView btnNextMonth = findViewById(R.id.btnNextMonth);

        documentId = getIntent().getStringExtra("documentId");
        pageType = getIntent().getStringExtra("pageType");
        if (pageType == null) pageType = "Laundry";

        if (documentId != null) {
            isEditMode = true;
            selectedDay = getIntent().getStringExtra("date");
            onTime = getIntent().getStringExtra("onTime");
            offTime = getIntent().getStringExtra("offTime");

            if (onTime != null) {
                etOnTime.setText(onTime.replace(" AM", "").replace(" PM", ""));
                onAmPm = onTime.contains("PM") ? "PM" : "AM";
                tvOnAmPm.setText(onAmPm);
            }
            if (offTime != null) {
                etOffTime.setText(offTime.replace(" AM", "").replace(" PM", ""));
                offAmPm = offTime.contains("PM") ? "PM" : "AM";
                tvOffAmPm.setText(offAmPm);
            }
        }

        updateMonthHeader();
        generateDateButtons();

        btnPrevMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        etOnTime.setOnClickListener(v -> openTimePicker(true));
        etOffTime.setOnClickListener(v -> openTimePicker(false));

        tvOnAmPm.setOnClickListener(v -> toggleAmPm(true));
        tvOffAmPm.setOnClickListener(v -> toggleAmPm(false));

        btnClearAll.setOnClickListener(v -> clearSelections());
        btnConfirmSchedule.setOnClickListener(v -> saveSchedule());

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

    }

    private void toggleAmPm(boolean isOnTime) {
        if (isOnTime) {
            onAmPm = onAmPm.equals("AM") ? "PM" : "AM";
            tvOnAmPm.setText(onAmPm);
        } else {
            offAmPm = offAmPm.equals("AM") ? "PM" : "AM";
            tvOffAmPm.setText(offAmPm);
        }
    }

    private void clearSelections() {
        selectedDay = "";
        onTime = "";
        offTime = "";
        etOnTime.setText("");
        etOffTime.setText("");
        Toast.makeText(this, "Cleared all selections", Toast.LENGTH_SHORT).show();
    }

    private void changeMonth(int offset) {
        currentMonth.add(Calendar.MONTH, offset);
        updateMonthHeader();
        generateDateButtons();
    }

    private void updateMonthHeader() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));
    }

    private void generateDateButtons() {
        layoutDateSelector.removeAllViews();
        Calendar tempCal = (Calendar) currentMonth.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= maxDay; day++) {
            final int selectedDayInt = day;
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(day));
            tv.setTextSize(16);
            tv.setPadding(28, 16, 28, 16);
            tv.setBackgroundResource(R.drawable.bg_date_unselected);

            if (!selectedDay.isEmpty() && selectedDay.equals(String.valueOf(day))) {
                tv.setBackgroundResource(R.drawable.bg_date_selected);
            }

            tv.setOnClickListener(v -> {
                selectedDay = String.valueOf(selectedDayInt);
                refreshDateSelection(selectedDayInt);
            });

            layoutDateSelector.addView(tv);
        }
    }

    private void refreshDateSelection(int selected) {
        for (int i = 0; i < layoutDateSelector.getChildCount(); i++) {
            TextView tv = (TextView) layoutDateSelector.getChildAt(i);
            if (tv.getText().toString().equals(String.valueOf(selected))) {
                tv.setBackgroundResource(R.drawable.bg_date_selected);
            } else {
                tv.setBackgroundResource(R.drawable.bg_date_unselected);
            }
        }
    }

    private void openTimePicker(boolean isOnTime) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    if (isOnTime) {
                        etOnTime.setText(formattedTime);
                        onTime = formattedTime;
                    } else {
                        etOffTime.setText(formattedTime);
                        offTime = formattedTime;
                    }
                },
                hour,
                minute,
                DateFormat.is24HourFormat(this)
        );
        timePicker.show();
    }

    // 🔥 Tentukan activity tujuan berdasarkan jenis page
    private Class<?> getTargetActivity() {
        switch (pageType) {
            case "Laundry":
                return LaundryActivity.class;
            case "Lighting":
                return LightingActivity.class;
            case "Water":
                return WaterActivity.class;
            case "Lock":
                return LockActivity.class;
            default:
                return MainActivity.class;
        }
    }

    private void saveSchedule() {
        if (selectedDay.isEmpty() || onTime.isEmpty() || offTime.isEmpty()) {
            Toast.makeText(this, "Please select date and times", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("pageType", pageType);
        schedule.put("date", selectedDay + " " + tvMonthYear.getText().toString());
        schedule.put("onTime", onTime + " " + onAmPm);
        schedule.put("offTime", offTime + " " + offAmPm);
        schedule.put("isActive", true);

        if (isEditMode && documentId != null) {

            db.collection("schedules").document(documentId)
                    .update(schedule)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Schedule updated!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ScheduleActivity.this, getTargetActivity());
                        intent.putExtra("pageType", pageType);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } else {

            db.collection("schedules")
                    .add(schedule)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Schedule saved!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ScheduleActivity.this, getTargetActivity());
                        intent.putExtra("pageType", pageType);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
