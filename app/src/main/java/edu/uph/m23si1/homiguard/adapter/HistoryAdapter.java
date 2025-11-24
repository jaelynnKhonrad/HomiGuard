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

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).tvHeader.setText(item.getHeaderTitle());
        }

        else if (holder instanceof ItemHolder) {
            ItemHolder ih = (ItemHolder) holder;

            ih.tvDevice.setText(item.getDevice());
            if (item.getDevice().equalsIgnoreCase("water level")) {

                // Water Level → pakai percent + cm
                String text = item.getPercent() + "% (" + item.getLevelCm() + " cm)";
                ih.tvValue.setText(text);

            } else {

                // Device lain → tetap pakai value
                ih.tvValue.setText(item.getValue());
            }

            // Format time
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(item.getTimestamp());
            ih.tvTime.setText(time);

            // Set icon sesuai device
            switch (item.getDevice().toLowerCase()) {
                case "lock":
                    ih.imgIcon.setImageResource(R.drawable.lock);
                    break;
                case "lighting":
                    ih.imgIcon.setImageResource(R.drawable.lighting);
                    break;
                case "water level":
                    ih.imgIcon.setImageResource(R.drawable.tank);
                    break;
                case "laundry":
                    ih.imgIcon.setImageResource(R.drawable.rain);
                    break;
                default:
                    ih.imgIcon.setImageResource(R.drawable.bg_card);
                    break;
            }
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
