package edu.uph.m23si1.homiguard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;

import java.util.List;

import edu.uph.m23si1.homiguard.R;
import edu.uph.m23si1.homiguard.model.ScheduleItem;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<ScheduleItem> scheduleList;
    private DatabaseReference deviceRef;
    private DatabaseReference scheduleRef;

    public ScheduleAdapter(List<ScheduleItem> scheduleList,
                           DatabaseReference deviceRef,
                           DatabaseReference scheduleRef) {
        this.scheduleList = scheduleList;
        this.deviceRef = deviceRef;
        this.scheduleRef = scheduleRef;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {

        ScheduleItem item = scheduleList.get(position);
        String type = item.getPageType().toLowerCase();

        // HANDLE DEVICE NAME VISIBILITY
        if (type.equals("laundry")) {
            // Laundry hanya 1 device → sembunyikan
            holder.tvDeviceName.setVisibility(View.GONE);
        }
        else if (type.equals("lighting")) {
            // Lighting → tampilkan nama ruangan
            holder.tvDeviceName.setVisibility(View.VISIBLE);
            holder.tvDeviceName.setText(item.getDeviceName());
        }
        else {
            // Lock, water level → biasanya tidak perlu tampilkan
            holder.tvDeviceName.setVisibility(View.GONE);
        }

        // SET DATE
        holder.tvDate.setText(item.getDate());

        // SET TIME
        String combinedTime = item.getOnTime() + " - " + item.getOffTime();
        holder.tvTime.setText(combinedTime);

        // REMOVE OLD LISTENER
        holder.switchActive.setOnCheckedChangeListener(null);

        // SET ACTIVE SWITCH
        holder.switchActive.setChecked(item.isActive());

        // UPDATE ACTIVE KE FIREBASE
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {

            item.setActive(isChecked);

            scheduleRef.child(item.getId()).child("active")
                    .setValue(isChecked)
                    .addOnSuccessListener(a ->
                            Toast.makeText(buttonView.getContext(),
                                    isChecked ? "Schedule activated" : "Schedule deactivated",
                                    Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(buttonView.getContext(),
                                    "Failed to update schedule",
                                    Toast.LENGTH_SHORT).show()
                    );
        });

        // DELETE BUTTON
        holder.btnDelete.setOnClickListener(v -> {
            scheduleRef.child(item.getId()).removeValue()
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(v.getContext(),
                                    "Schedule deleted",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(v.getContext(),
                                    "Failed to delete schedule",
                                    Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvDate, tvTime;
        Switch switchActive;
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            switchActive = itemView.findViewById(R.id.switchActive);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}