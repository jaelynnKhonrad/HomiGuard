package edu.uph.m23si1.homiguard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import edu.uph.m23si1.homiguard.R;
import edu.uph.m23si1.homiguard.model.HistoryModel;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<HistoryModel> list;

    public HistoryAdapter(List<HistoryModel> list) {
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isHeader() ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header, parent, false);
            return new HeaderHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        HistoryModel item = list.get(position);

        // ✅ TAMBAHKAN LOG INI:
        android.util.Log.d("HistoryAdapter", "Binding position " + position +
                ", isHeader=" + item.isHeader() +
                ", device=" + item.getDevice() +
                ", value=" + item.getValue());

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).tvHeader.setText(item.getHeaderTitle());
            return;
        }

        ItemHolder ih = (ItemHolder) holder;

        String device = item.getDevice() == null ? "" : item.getDevice();
        String deviceName = item.getDeviceName() == null ? "" : item.getDeviceName();
        String value = item.getValue() == null ? "-" : item.getValue();

        // ================= DEVICE TEXT =================
        if (device.equalsIgnoreCase("lighting")) {
            ih.tvDevice.setText("Lighting - " + deviceName);
        } else if (!device.isEmpty()) {
            ih.tvDevice.setText(device);
        } else {
            ih.tvDevice.setText("Unknown Device");
        }

        // ================= VALUE TEXT =================
        if (device.equalsIgnoreCase("water level")) {
            String text = item.getPercent() + "% (" + item.getLevelCm() + " cm)";
            ih.tvValue.setText(text);
        } else {
            ih.tvValue.setText(value);
        }

        // ================= TIME =================
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(item.getTimestamp());
        ih.tvTime.setText(time);

        // ================= ICON (FULL LIGHTING TIDAK DIPOTONG) =================
        if (device.isEmpty()) {
            ih.imgIcon.setImageResource(R.drawable.bg_card);
            return;
        }

        switch (device.toLowerCase()) {
            case "lock":
                ih.imgIcon.setImageResource(R.drawable.lock);
                break;

            case "lighting":
                if (deviceName != null && !deviceName.isEmpty()) {  // ✅ GANTI device → deviceName
                    String room = deviceName.toLowerCase().trim();  // ✅ GANTI device → deviceName

                    if (room.contains("bedroom")) {
                        ih.imgIcon.setImageResource(R.drawable.tank);
                    } else if (room.contains("livingroom")) {
                        ih.imgIcon.setImageResource(R.drawable.living);
                    } else if (room.contains("kitchen")) {
                        ih.imgIcon.setImageResource(R.drawable.kitchen);
                    } else if (room.contains("bathroom")) {
                        ih.imgIcon.setImageResource(R.drawable.toilet);
                    } else {
                        ih.imgIcon.setImageResource(R.drawable.lighting);
                    }
                } else {
                    ih.imgIcon.setImageResource(R.drawable.lighting);
                }
                break;  // ✅ TAMBAH break;

            case "water level":
                ih.imgIcon.setImageResource(R.drawable.tank);
                break;

            case "laundry":
                String status = value.toLowerCase();
                if (status.equals("it's not raining")) {
                    ih.imgIcon.setImageResource(R.drawable.sun); // gambar hujan
                } else {
                    ih.imgIcon.setImageResource(R.drawable.rain); // gambar cerah / tidak hujan
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        TextView tvDevice, tvValue, tvTime;
        ImageView imgIcon;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);

            imgIcon = itemView.findViewById(R.id.imgIcon);
            tvDevice = itemView.findViewById(R.id.tvDevice);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}