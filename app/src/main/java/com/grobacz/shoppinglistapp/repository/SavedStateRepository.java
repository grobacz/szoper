package com.grobacz.shoppinglistapp.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.grobacz.shoppinglistapp.AppDatabase;
import com.grobacz.shoppinglistapp.dao.SavedStateDao;
import com.grobacz.shoppinglistapp.model.SavedState;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SavedStateRepository {
    private SavedStateDao savedStateDao;
    private LiveData<List<SavedState>> allSavedStates;

    public SavedStateRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        savedStateDao = db.savedStateDao();
        allSavedStates = savedStateDao.getAll();
    }

    public LiveData<List<SavedState>> getAllSavedStates() {
        return allSavedStates;
    }

    public void insert(SavedState state) {
        new InsertAsyncTask(savedStateDao).execute(state);
    }

    public void delete(SavedState state) {
        new DeleteAsyncTask(savedStateDao).execute(state);
    }

    public void deleteById(int id) {
        new DeleteByIdAsyncTask(savedStateDao).execute(id);
    }

    public SavedState getById(int id) {
        try {
            return new GetByIdAsyncTask(savedStateDao).execute(id).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class InsertAsyncTask extends AsyncTask<SavedState, Void, Void> {
        private SavedStateDao asyncTaskDao;

        InsertAsyncTask(SavedStateDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final SavedState... params) {
            asyncTaskDao.insert(params[0]);
            return null;
        }
    }

    private static class DeleteAsyncTask extends AsyncTask<SavedState, Void, Void> {
        private SavedStateDao asyncTaskDao;

        DeleteAsyncTask(SavedStateDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final SavedState... params) {
            asyncTaskDao.delete(params[0]);
            return null;
        }
    }

    private static class DeleteByIdAsyncTask extends AsyncTask<Integer, Void, Void> {
        private SavedStateDao asyncTaskDao;

        DeleteByIdAsyncTask(SavedStateDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            asyncTaskDao.deleteById(params[0]);
            return null;
        }
    }

    private static class GetByIdAsyncTask extends AsyncTask<Integer, Void, SavedState> {
        private SavedStateDao asyncTaskDao;

        GetByIdAsyncTask(SavedStateDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected SavedState doInBackground(final Integer... params) {
            return asyncTaskDao.getById(params[0]);
        }
    }
}
