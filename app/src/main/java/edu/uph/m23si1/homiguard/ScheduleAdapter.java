package edu.uph.m23si1.homiguard;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import edu.uph.m23si1.homiguard.model.ScheduleItem;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<ScheduleItem> scheduleList;
    private final FirebaseFirestore db;

    public ScheduleAdapter(List<ScheduleItem> scheduleList, FirebaseFirestore db) {
        this.scheduleList = scheduleList;
        this.db = db;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = scheduleList.get(position);

        holder.tvDate.setText(item.getDate());
        holder.tvTime.setText(item.getOnTime() + " - " + item.getOffTime());
        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(item.isActive());

        // ✅ Toggle schedule active/inactive
        holder.switchActive.setOnCheckedChangeListener((button, isChecked) -> {
            item.setActive(isChecked);
            db.collection("schedules").document(item.getId())
                    .update("isActive", isChecked)
                    .addOnFailureListener(e -> Toast.makeText(
                            holder.itemView.getContext(),
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show());
        });

        // ✅ Delete schedule
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            db.collection("schedules").document(item.getId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        scheduleList.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                        notifyItemRangeChanged(currentPosition, scheduleList.size());
                        Toast.makeText(holder.itemView.getContext(),
                                "Schedule deleted",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(
                            holder.itemView.getContext(),
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show());
        });

        // ✅ Klik pada card → Edit Schedule
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ScheduleActivity.class);
            intent.putExtra("documentId", item.getId());
            intent.putExtra("pageType", item.getPageType());
            intent.putExtra("date", item.getDate());
            intent.putExtra("onTime", item.getOnTime());
            intent.putExtra("offTime", item.getOffTime());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime;
        Switch switchActive;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            switchActive = itemView.findViewById(R.id.switchActive);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
