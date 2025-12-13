package edu.uph.m23si1.homiguard.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.uph.m23si1.homiguard.R;

public class StatisticFragment extends Fragment {

    private TextView txvSuhu, txvKelembapan;
    private LineChart chartTemp, chartHumidity;

    private final List<Entry> suhuEntries = new ArrayList<>();
    private final List<Entry> kelembapanEntries = new ArrayList<>();
    private final List<String> waktuLabels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        txvSuhu = view.findViewById(R.id.txvSuhu);
        txvKelembapan = view.findViewById(R.id.txvKelembapan);
        chartTemp = view.findViewById(R.id.chartTemp);
        chartHumidity = view.findViewById(R.id.chartHumidity);

        startThingSpeakFetch();

        return view;
    }

    // ================================
    //      FETCH DATA THINGSPEAK
    // ================================
    private void startThingSpeakFetch() {

        new Thread(() -> {

            // Ambil data 24 jam terakhir, dengan rata-rata per jam
            String urlStr = "https://api.thingspeak.com/channels/3194435/feeds.json?results=1440&average=60";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray feeds = json.getJSONArray("feeds");

                // Maps untuk grouping data per jam
                Map<String, List<Double>> suhuPerJam = new LinkedHashMap<>();
                Map<String, List<Double>> kelembapanPerJam = new LinkedHashMap<>();

                for (int i = 0; i < feeds.length(); i++) {
                    JSONObject feed = feeds.getJSONObject(i);

                    double suhu = feed.optDouble("field1", -1);
                    double kelembapan = feed.optDouble("field2", -1);

                    String created = feed.getString("created_at");
                    // Format: 2024-01-15T07:30:00Z
                    String tanggal = created.substring(8, 10);  // DD
                    String jam = created.substring(11, 13);     // HH
                    String key = tanggal + "-" + jam;

                    if (suhu != -1) {
                        suhuPerJam.computeIfAbsent(key, k -> new ArrayList<>()).add(suhu);
                    }
                    if (kelembapan != -1) {
                        kelembapanPerJam.computeIfAbsent(key, k -> new ArrayList<>()).add(kelembapan);
                    }
                }

                // Clear old data
                suhuEntries.clear();
                kelembapanEntries.clear();
                waktuLabels.clear();

                // Convert map → chart entries (dengan rata-rata)
                int index = 0;
                for (Map.Entry<String, List<Double>> entry : suhuPerJam.entrySet()) {
                    String[] parts = entry.getKey().split("-");
                    String label = parts[1] + ":00";  // Hanya jam

                    // Hitung rata-rata
                    List<Double> values = entry.getValue();
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);

                    waktuLabels.add(label);
                    suhuEntries.add(new Entry(index, (float) avg));
                    index++;
                }

                index = 0;
                for (Map.Entry<String, List<Double>> entry : kelembapanPerJam.entrySet()) {
                    List<Double> values = entry.getValue();
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);

                    kelembapanEntries.add(new Entry(index, (float) avg));
                    index++;
                }

                requireActivity().runOnUiThread(this::updateCharts);

            } catch (Exception e) {
                Log.e("ThingSpeak", "ERROR: " + e);
            }
        }).start();
    }

    // ================================
    //           UPDATE CHART
    // ================================
    private void updateCharts() {

        // === SUHU ===
        LineDataSet setSuhu = new LineDataSet(suhuEntries, "Suhu (°C)");
        setSuhu.setColor(Color.RED);
        setSuhu.setCircleColor(Color.RED);
        setSuhu.setLineWidth(2f);
        setSuhu.setCircleRadius(4f);
        setSuhu.setDrawValues(false);
        chartTemp.setData(new LineData(setSuhu));
        setupXAxis(chartTemp);
        chartTemp.getDescription().setEnabled(false);
        chartTemp.invalidate();

        // === KELEMBAPAN ===
        LineDataSet setKelembapan = new LineDataSet(kelembapanEntries, "Kelembapan (%)");
        setKelembapan.setColor(Color.BLUE);
        setKelembapan.setCircleColor(Color.BLUE);
        setKelembapan.setLineWidth(2f);
        setKelembapan.setCircleRadius(4f);
        setKelembapan.setDrawValues(false);
        chartHumidity.setData(new LineData(setKelembapan));
        setupXAxis(chartHumidity);
        chartHumidity.getDescription().setEnabled(false);
        chartHumidity.invalidate();

        // Update text last value
        if (!suhuEntries.isEmpty()) {
            txvSuhu.setText(String.format("%.1f°C", suhuEntries.get(suhuEntries.size() - 1).getY()));
        }
        if (!kelembapanEntries.isEmpty()) {
            txvKelembapan.setText(String.format("%.1f%%", kelembapanEntries.get(kelembapanEntries.size() - 1).getY()));
        }
    }

    private void setupXAxis(LineChart chart) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(waktuLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
    }
}