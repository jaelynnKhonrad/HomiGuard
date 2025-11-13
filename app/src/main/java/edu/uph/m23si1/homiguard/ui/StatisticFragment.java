package edu.uph.m23si1.homiguard.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.uph.m23si1.homiguard.R;

public class StatisticFragment extends Fragment {

    private TextView txvSuhu, txvKelembapan, txvIntensitas, txvHujan;
    private LineChart chartTemp, chartHumidity, chartLight, chartRain;
    private Spinner spinnerTemp, spinnerHumidity, spinnerLight, spinnerRain;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final List<Entry> suhuEntries = new ArrayList<>();
    private final List<Entry> kelembapanEntries = new ArrayList<>();
    private final List<Entry> cahayaEntries = new ArrayList<>();
    private final List<Entry> hujanEntries = new ArrayList<>();
    private final List<String> waktuLabels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        // Bind views
        txvSuhu = view.findViewById(R.id.txvSuhu);
        txvKelembapan = view.findViewById(R.id.txvKelembapan);
        txvIntensitas = view.findViewById(R.id.txvIntensitas);
        txvHujan = view.findViewById(R.id.txvHujan);

        chartTemp = view.findViewById(R.id.chartTemp);
        chartHumidity = view.findViewById(R.id.chartHumidity);
        chartLight = view.findViewById(R.id.chartLight);
        chartRain = view.findViewById(R.id.charRain);

        spinnerTemp = view.findViewById(R.id.spinnerTemp);
        spinnerHumidity = view.findViewById(R.id.spinnerHumidity);
        spinnerLight = view.findViewById(R.id.spinnerLight);
        spinnerRain = view.findViewById(R.id.spinnerRain);

        startRealtimeListener();
        return view;
    }

    private void startRealtimeListener() {
        db.collection("sensorData")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(20)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Gagal ambil data realtime", e);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) return;

                    suhuEntries.clear();
                    kelembapanEntries.clear();
                    cahayaEntries.clear();
                    hujanEntries.clear();
                    waktuLabels.clear();

                    int index = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Double suhu = doc.getDouble("suhu");
                        Double kelembapan = doc.getDouble("kelembapan");
                        Double cahaya = doc.getDouble("cahaya");
                        Double hujan = doc.getDouble("hujan"); // pastikan field Firestore-nya "hujan"
                        Timestamp time = doc.getTimestamp("timestamp");

                        if (time != null) {
                            Date date = time.toDate();
                            String label = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
                            waktuLabels.add(label);
                        } else {
                            waktuLabels.add(String.valueOf(index));
                        }

                        if (suhu != null) suhuEntries.add(new Entry(index, suhu.floatValue()));
                        if (kelembapan != null) kelembapanEntries.add(new Entry(index, kelembapan.floatValue()));
                        if (cahaya != null) cahayaEntries.add(new Entry(index, cahaya.floatValue()));
                        if (hujan != null) hujanEntries.add(new Entry(index, hujan.floatValue()));

                        index++;
                    }

                    updateCharts();
                });
    }

    private void updateCharts() {
        // === SUHU ===
        LineDataSet setSuhu = new LineDataSet(suhuEntries, "Suhu (°C)");
        setSuhu.setColor(Color.RED);
        setSuhu.setCircleColor(Color.RED);
        setSuhu.setValueTextColor(Color.BLACK);
        setSuhu.setValueTextSize(9f);
        chartTemp.setData(new LineData(setSuhu));
        setupXAxis(chartTemp);
        chartTemp.getDescription().setText("Grafik Suhu");
        chartTemp.invalidate();

        // === KELEMBAPAN ===
        LineDataSet setKelembapan = new LineDataSet(kelembapanEntries, "Kelembapan (%)");
        setKelembapan.setColor(Color.BLUE);
        setKelembapan.setCircleColor(Color.BLUE);
        setKelembapan.setValueTextColor(Color.BLACK);
        setKelembapan.setValueTextSize(9f);
        chartHumidity.setData(new LineData(setKelembapan));
        setupXAxis(chartHumidity);
        chartHumidity.getDescription().setText("Grafik Kelembapan");
        chartHumidity.invalidate();

        // === CAHAYA ===
        LineDataSet setCahaya = new LineDataSet(cahayaEntries, "Cahaya (Lux)");
        setCahaya.setColor(ColorTemplate.rgb("#FFC107"));
        setCahaya.setCircleColor(ColorTemplate.rgb("#FFC107"));
        setCahaya.setValueTextColor(Color.BLACK);
        setCahaya.setValueTextSize(9f);
        chartLight.setData(new LineData(setCahaya));
        setupXAxis(chartLight);
        chartLight.getDescription().setText("Grafik Cahaya");
        chartLight.invalidate();

        // === HUJAN ===
        LineDataSet setHujan = new LineDataSet(hujanEntries, "Sensor Hujan");
        setHujan.setColor(ColorTemplate.rgb("#2196F3"));
        setHujan.setCircleColor(ColorTemplate.rgb("#2196F3"));
        setHujan.setValueTextColor(Color.BLACK);
        setHujan.setValueTextSize(9f);
        chartRain.setData(new LineData(setHujan));
        setupXAxis(chartRain);
        chartRain.getDescription().setText("Grafik Sensor Hujan");
        chartRain.invalidate();

        // === Update nilai terakhir ===
        if (!suhuEntries.isEmpty()) {
            txvSuhu.setText(String.format(Locale.getDefault(), "%.1f°C",
                    suhuEntries.get(suhuEntries.size() - 1).getY()));
        }
        if (!kelembapanEntries.isEmpty()) {
            txvKelembapan.setText(String.format(Locale.getDefault(), "%.1f%%",
                    kelembapanEntries.get(kelembapanEntries.size() - 1).getY()));
        }
        if (!cahayaEntries.isEmpty()) {
            txvIntensitas.setText(String.format(Locale.getDefault(), "%.0f lx",
                    cahayaEntries.get(cahayaEntries.size() - 1).getY()));
        }
        if (!hujanEntries.isEmpty()) {
            float latest = hujanEntries.get(hujanEntries.size() - 1).getY();
            if (latest == 0) txvHujan.setText("Tidak Hujan");
            else if (latest == 1) txvHujan.setText("Hujan Ringan");
            else txvHujan.setText("Hujan Lebat");
        }
    }

    private void setupXAxis(LineChart chart) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(waktuLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
    }
}
