package edu.uph.m23si1.homiguard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.uph.m23si1.homiguard.R;
import edu.uph.m23si1.homiguard.model.HistoryModel;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryModel> historyList;

    public HistoryAdapter(List<HistoryModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryModel item = historyList.get(position);
        holder.tvStatus.setText(item.getStatus());
        holder.tvUnlockTime.setText(item.getUnlock());
        holder.tvLockTime.setText(item.getLock());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvUnlockTime, tvLockTime;
        ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUnlockTime = itemView.findViewById(R.id.tvUnlockTime);
            tvLockTime = itemView.findViewById(R.id.tvLockTime);
//            icon = itemView.findViewById(R.id.icon_rfid);
        }
    }
}
