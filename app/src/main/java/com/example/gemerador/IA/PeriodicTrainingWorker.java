package com.example.gemerador.IA;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
public class PeriodicTrainingWorker extends Worker {
    public PeriodicTrainingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Aquí llamarías al método de entrenamiento
        // Asegúrate de que puedas acceder a tu TicketReportActivity o al modelo desde aquí
        return Result.success();
    }
    public static void schedulePeriodicTraining(Context context) {
        PeriodicWorkRequest trainingWork =
                new PeriodicWorkRequest.Builder(PeriodicTrainingWorker.class, 1, TimeUnit.DAYS)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_training",
                ExistingPeriodicWorkPolicy.REPLACE,
                trainingWork);
    }
}