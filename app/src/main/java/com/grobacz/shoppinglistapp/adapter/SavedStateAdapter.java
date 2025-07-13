package com.grobacz.shoppinglistapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.grobacz.shoppinglistapp.R;
import com.grobacz.shoppinglistapp.model.SavedState;
import com.grobacz.shoppinglistapp.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class SavedStateAdapter extends ListAdapter<SavedState, SavedStateAdapter.SavedStateViewHolder> {
    
    private int selectedPosition = -1;
    private OnItemClickListener listener;
    private List<SavedState> savedStates = new ArrayList<>();
    
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onDeleteClick(int position);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public SavedStateAdapter() {
        super(DIFF_CALLBACK);
    }
    
    private static final DiffUtil.ItemCallback<SavedState> DIFF_CALLBACK = new DiffUtil.ItemCallback<SavedState>() {
        @Override
        public boolean areItemsTheSame(@NonNull SavedState oldItem, @NonNull SavedState newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull SavedState oldItem, @NonNull SavedState newItem) {
            return oldItem.getName().equals(newItem.getName()) && 
                   oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };
    
    @NonNull
    @Override
    public SavedStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_state, parent, false);
        return new SavedStateViewHolder(itemView, listener);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SavedStateViewHolder holder, int position) {
        SavedState savedState = getItem(position);
        
        // Set the name
        holder.textViewName.setText(savedState.getName());
        
        // Format the timestamp using DateTimeUtils
        String formattedDate = DateTimeUtils.getRelativeTimeSpanString(savedState.getTimestamp());
        holder.textViewDate.setText(formattedDate);
        
        // Highlight the selected item
        holder.itemView.setSelected(selectedPosition == position);
        
        // Highlight the selected item
        holder.itemView.setSelected(selectedPosition == position);
        
        // Show/hide delete button based on selection
        holder.buttonDelete.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);
    }
    
    @Nullable
    public SavedState getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < getItemCount()) {
            return getItem(selectedPosition);
        }
        return null;
    }
    
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }
    
    @Override
    public void submitList(List<SavedState> list) {
        super.submitList(list);
    }
    
    static class SavedStateViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final TextView textViewDate;
        private final ImageButton buttonDelete;
        
        public SavedStateViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewStateName);
            textViewDate = itemView.findViewById(R.id.textViewStateDate);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(v, position);
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(position);
                }
            });
        }
    }
}
