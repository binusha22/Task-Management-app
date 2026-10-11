package com.gms.taskmanagement.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.gms.taskmanagement.R;
import com.gms.taskmanagement.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final Context context;
    private final List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public TaskAdapter(Context context, OnTaskClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        this.tasks.clear();
        if (newTasks != null) {
            this.tasks.addAll(newTasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTicketNumber.setText(task.getTicketNumber() != null ? task.getTicketNumber() : "");
        holder.tvJobType.setText(task.getJobType() != null ? task.getJobType() : "");
        holder.tvDescription.setText(task.getDescription() != null ? task.getDescription() : "");
        holder.tvDepartment.setText(task.getDepartment() != null ? task.getDepartment() : "");
        holder.tvDateOfPerform.setText(task.getDateOfPerform() != null ? task.getDateOfPerform() : "");
        holder.tvSubmittedTimestamp.setText("Submitted: " + (task.getTimestamp() != null ? task.getTimestamp() : ""));

        String status = task.getStatus() != null ? task.getStatus() : "Pending";
        holder.tvStatusChip.setText(status);

        if ("Completed".equalsIgnoreCase(status)) {
            holder.tvStatusChip.setTextColor(ContextCompat.getColor(context, R.color.status_completed));
            holder.tvStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_completed_bg));
        } else if ("In Progress".equalsIgnoreCase(status)) {
            holder.tvStatusChip.setTextColor(ContextCompat.getColor(context, R.color.status_inprogress));
            holder.tvStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_inprogress_bg));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            holder.tvStatusChip.setTextColor(ContextCompat.getColor(context, R.color.status_rejected));
            holder.tvStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_rejected_bg));
        } else {
            holder.tvStatusChip.setTextColor(ContextCompat.getColor(context, R.color.status_pending));
            holder.tvStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_pending_bg));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketNumber, tvStatusChip, tvJobType, tvDescription, tvDepartment, tvDateOfPerform, tvSubmittedTimestamp;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvJobType = itemView.findViewById(R.id.tvJobType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDateOfPerform = itemView.findViewById(R.id.tvDateOfPerform);
            tvSubmittedTimestamp = itemView.findViewById(R.id.tvSubmittedTimestamp);
        }
    }
}
