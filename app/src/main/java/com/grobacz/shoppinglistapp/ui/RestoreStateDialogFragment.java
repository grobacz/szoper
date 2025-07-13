package com.grobacz.shoppinglistapp.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grobacz.shoppinglistapp.R;
import com.grobacz.shoppinglistapp.adapter.SavedStateAdapter;
import com.grobacz.shoppinglistapp.model.SavedState;
import com.grobacz.shoppinglistapp.viewmodel.SavedStateViewModel;

import java.util.ArrayList;
import java.util.List;

public class RestoreStateDialogFragment extends DialogFragment {
    
    public interface OnStateRestoreListener {
        void onStateRestored(com.grobacz.shoppinglistapp.model.SavedState state);
    }
    
    private OnStateRestoreListener listener;
    private SavedStateViewModel viewModel;
    private SavedStateAdapter adapter;
    private Button buttonRestore;
    private TextView textViewEmpty;
    
    public static RestoreStateDialogFragment newInstance() {
        return new RestoreStateDialogFragment();
    }
    
    public void setRestoreStateListener(OnStateRestoreListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SavedStateViewModel.class);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_restore_state_dialog, container, false);
        
        // Initialize views
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_saved_states);
        textViewEmpty = view.findViewById(R.id.text_empty);
        buttonRestore = view.findViewById(R.id.button_restore);
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        
        // Setup RecyclerView
        adapter = new SavedStateAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Observe saved states
        viewModel.getAllSavedStates().observe(getViewLifecycleOwner(), this::onSavedStatesChanged);
        
        // Set up button click listeners
        buttonRestore.setOnClickListener(v -> onRestoreClick());
        buttonCancel.setOnClickListener(v -> dismiss());
        
        // Initially disable the restore button until a state is selected
        buttonRestore.setEnabled(false);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up adapter callbacks
        adapter.setOnItemClickListener(new SavedStateAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // Update UI when an item is selected
                adapter.setSelectedPosition(position);
                updateButtonState();
            }
            
            @Override
            public void onDeleteClick(int position) {
                // Get the item at the clicked position
                if (position >= 0 && position < adapter.getItemCount()) {
                    com.grobacz.shoppinglistapp.model.SavedState state = adapter.getCurrentList().get(position);
                    if (state != null) {
                        showDeleteConfirmation(state);
                    }
                }
            }
        });
        
        // Observe saved states
        viewModel.getAllSavedStates().observe(getViewLifecycleOwner(), states -> {
            if (adapter != null) {
                adapter.submitList(states);
                updateEmptyView(states);
                updateButtonState();
            }
        });
    }
    
    private void onSavedStatesChanged(List<com.grobacz.shoppinglistapp.model.SavedState> states) {
        if (states == null) {
            states = new ArrayList<>();
        }
        
        // Update the adapter with the new list
        if (adapter != null) {
            adapter.submitList(new ArrayList<>(states));
        }
        
        // Show empty view if no saved states
        updateEmptyView(states);
        
        // Disable restore button if no selection or no states
        updateButtonState();
    }
    
    private void updateEmptyView(List<com.grobacz.shoppinglistapp.model.SavedState> states) {
        if (textViewEmpty != null) {
            textViewEmpty.setVisibility(states == null || states.isEmpty() ? View.VISIBLE : View.GONE);
        }
        
        // Make sure the RecyclerView is visible if we have items
        if (getView() != null) {
            RecyclerView recyclerView = getView().findViewById(R.id.recycler_view_saved_states);
            if (recyclerView != null) {
                recyclerView.setVisibility(states != null && !states.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    private void updateButtonState() {
        // Enable the restore button if we have items and one is selected
        if (buttonRestore != null) {
            boolean hasItems = adapter != null && adapter.getItemCount() > 0;
            boolean hasSelection = adapter != null && adapter.getSelectedItem() != null;
            buttonRestore.setEnabled(hasItems && hasSelection);
        }
    }
    
    private void onRestoreClick() {
        if (adapter != null) {
            com.grobacz.shoppinglistapp.model.SavedState state = adapter.getSelectedItem();
            if (state != null && listener != null) {
                listener.onStateRestored(state);
                dismiss();
            } else {
                // Show error message if no state is selected
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Please select a state to restore", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void showDeleteConfirmation(com.grobacz.shoppinglistapp.model.SavedState state) {
        if (getContext() == null) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete State")
                .setMessage("Are you sure you want to delete this saved state?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteState(state))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    private void deleteState(com.grobacz.shoppinglistapp.model.SavedState state) {
        if (state != null && viewModel != null) {
            viewModel.delete(state);
            
            // Reset selection after deletion
            if (adapter != null) {
                adapter.setSelectedPosition(-1);
            }
            
            // Show a toast to confirm deletion
            if (getContext() != null) {
                String message = "Deleted: " + state.getName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
