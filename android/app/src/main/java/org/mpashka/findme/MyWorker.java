package org.mpashka.findme;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import timber.log.Timber;

public class MyWorker extends Worker {

    public MyWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Performing long running task in scheduled job");
        // TODO(developer): add long running task here.
        return Result.success();
    }
}
