package com.example.ilazar.mykeep.util;

import android.os.AsyncTask;
import android.util.Log;

public abstract class OkAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    public static final String TAG = OkAsyncTask.class.getSimpleName();

    public Exception backgroundException;

    @Override
    protected Result doInBackground(Params... params) {
        try {
            return tryInBackground(params);
        } catch (Exception ex) {
            Log.w(TAG, "Exception", ex);
            backgroundException = ex;
            return null;
        }
    }

    protected abstract Result tryInBackground(Params... params) throws Exception;

}
