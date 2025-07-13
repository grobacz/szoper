package com.grobacz.shoppinglistapp.ui;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.grobacz.shoppinglistapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DeleteConfirmationDialog extends DialogFragment {
    
    public interface DeleteConfirmationListener {
        void onConfirmDelete();
    }
    
    private DeleteConfirmationListener listener;
    private String itemName;
    
    public static DeleteConfirmationDialog newInstance(String itemName) {
        DeleteConfirmationDialog dialog = new DeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putString("itemName", itemName);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemName = getArguments().getString("itemName");
        }
    }
    
    public void setDeleteConfirmationListener(DeleteConfirmationListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        String message = getString(R.string.confirm_delete_state, itemName);
        builder.setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onConfirmDelete();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null);

        return builder.create();
    }
}
