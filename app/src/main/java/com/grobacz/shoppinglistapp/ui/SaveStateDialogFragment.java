package com.grobacz.shoppinglistapp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.grobacz.shoppinglistapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SaveStateDialogFragment extends DialogFragment {
    public interface SaveStateDialogListener {
        void onSaveClicked(String name);
    }

    private SaveStateDialogListener listener;
    private String currentName;
    private EditText editTextName;

    public static SaveStateDialogFragment newInstance(String currentName) {
        SaveStateDialogFragment fragment = new SaveStateDialogFragment();
        Bundle args = new Bundle();
        args.putString("current_name", currentName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentName = getArguments().getString("current_name", "");
        }
    }

    public void setSaveStateDialogListener(SaveStateDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_state, null);

        editTextName = view.findViewById(R.id.editTextStateName);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        if (currentName != null && !currentName.isEmpty()) {
            editTextName.setText(currentName);
            editTextName.setSelection(currentName.length());
        }

        builder.setView(view);
        final AlertDialog dialog = builder.create();
        
        buttonSave.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_name_provided, Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onSaveClicked(name);
            }
            dialog.dismiss();
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Show the keyboard when the dialog is shown
        if (editTextName != null) {
            editTextName.requestFocus();
        }
    }
}
