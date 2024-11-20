package com.example.gemerador.IA;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TicketPredictor {
    private static final String TAG = "TicketPredictor";
    private static final String MODEL_FILE = "ticket_model.tflite";
    private Interpreter tflite;
    private Context context;

    public TicketPredictor(Context context) throws IOException {
        this.context = context;
        try {
            tflite = new Interpreter(loadModelFile(context, MODEL_FILE));
            Log.d(TAG, "Modelo cargado exitosamente");
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar el modelo: " + e.getMessage());
            listAvailableAssets(context);
            throw e;
        }
    }

    public float predict(TicketMLData ticket) {
        try {
            float[][] input = new float[1][3];
            float[] ticketData = ticket.toFloatArray();

            // Verificar que tenemos los datos correctos
            if (ticketData.length != 3) {
                throw new IllegalArgumentException("El modelo espera 3 features, pero se recibieron " + ticketData.length);
            }

            // Copiar los datos al array de entrada
            System.arraycopy(ticketData, 0, input[0], 0, 3);

            // Preparar el array de salida
            float[][] output = new float[1][1];

            // Hacer la predicción
            tflite.run(input, output);

            Log.d(TAG, "Predicción exitosa: " + output[0][0]);
            return output[0][0];

        } catch (Exception e) {
            Log.e(TAG, "Error al hacer predicción: " + e.getMessage());
            throw new RuntimeException("Error al hacer predicción: " + e.getMessage(), e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            fileDescriptor.close();
            inputStream.close();
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar el archivo del modelo: " + e.getMessage());
            throw e;
        }
    }

    private void listAvailableAssets(Context context) {
        try {
            String[] files = context.getAssets().list("");
            Log.d(TAG, "Archivos disponibles en assets:");
            for (String file : files) {
                Log.d(TAG, "- " + file);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al listar assets: " + e.getMessage());
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}