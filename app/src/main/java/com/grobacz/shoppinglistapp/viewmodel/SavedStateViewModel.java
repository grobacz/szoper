package com.grobacz.shoppinglistapp.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.grobacz.shoppinglistapp.model.SavedState;
import com.grobacz.shoppinglistapp.repository.SavedStateRepository;

import java.util.List;

public class SavedStateViewModel extends AndroidViewModel {
    private SavedStateRepository repository;
    private LiveData<List<SavedState>> allSavedStates;

    public SavedStateViewModel(Application application) {
        super(application);
        repository = new SavedStateRepository(application);
        allSavedStates = repository.getAllSavedStates();
    }

    public LiveData<List<SavedState>> getAllSavedStates() {
        return allSavedStates;
    }

    public void insert(SavedState state) {
        repository.insert(state);
    }

    public void delete(SavedState state) {
        repository.delete(state);
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public SavedState getById(int id) {
        return repository.getById(id);
    }
}
