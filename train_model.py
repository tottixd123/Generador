import pandas as pd
import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
import os

def ensure_directory_exists(file_path):
    directory = os.path.dirname(file_path)
    if not os.path.exists(directory):
        os.makedirs(directory)
        print(f"Creado directorio: {directory}")

def load_and_preprocess_data():
    try:
        # Cargar datos desde tickets_data.csv
        data = pd.read_csv('tickets_data.csv', sep=';', encoding='utf-8')
        print(f"Datos cargados: {len(data)} registros")

        # Convertir variables categóricas a numéricas
        le_area = LabelEncoder()
        le_prioridad = LabelEncoder()
        le_status = LabelEncoder()

        # Transformar las columnas categóricas
        data['Area_Problema_encoded'] = le_area.fit_transform(data['Area_Problema'])
        data['Prioridad_encoded'] = le_prioridad.fit_transform(data['Prioridad'])
        data['Estatus_encoded'] = le_status.fit_transform(data['Estatus'])

        # Guardar los mapeos de las etiquetas para uso posterior
        label_mappings = {
            'area_mapping': dict(zip(le_area.classes_, le_area.transform(le_area.classes_))),
            'prioridad_mapping': dict(zip(le_prioridad.classes_, le_prioridad.transform(le_prioridad.classes_))),
            'status_mapping': dict(zip(le_status.classes_, le_status.transform(le_status.classes_)))
        }

        # Seleccionar features y target
        features = ['Area_Problema_encoded', 'Prioridad_encoded', 'Trabajador']
        target = 'Tiempo_resolució'

        X = data[features]
        y = data[target]

        # Dividir datos en entrenamiento y prueba
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        # Normalizar los datos
        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        print("Preprocesamiento completado exitosamente")
        return X_train_scaled, X_test_scaled, y_train, y_test, label_mappings, scaler
    except Exception as e:
        print(f"Error en load_and_preprocess_data: {str(e)}")
        raise

def create_model(input_shape):
    try:
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(32, activation='relu', input_shape=input_shape),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(16, activation='relu'),
            tf.keras.layers.Dense(1)
        ])

        model.compile(optimizer='adam',
                      loss='mse',
                      metrics=['mae'])

        print("Modelo creado exitosamente")
        return model
    except Exception as e:
        print(f"Error en create_model: {str(e)}")
        raise

def train_and_save_model():
    try:
        # Definir la ruta del modelo
        model_path = os.path.join('app', 'src', 'main', 'assets', 'ticket_model.tflite')

        # Asegurar que el directorio existe
        ensure_directory_exists(model_path)

        print("Iniciando entrenamiento del modelo...")

        # Cargar y preprocesar datos
        X_train, X_test, y_train, y_test, label_mappings, scaler = load_and_preprocess_data()

        # Crear y entrenar el modelo
        model = create_model((X_train.shape[1],))

        history = model.fit(X_train, y_train,
                            epochs=50,
                            batch_size=4,
                            validation_split=0.2,
                            verbose=1)

        print("Modelo entrenado exitosamente")

        # Convertir a TFLite
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        tflite_model = converter.convert()

        # Guardar el modelo
        with open(model_path, 'wb') as f:
            f.write(tflite_model)

        print(f"Modelo guardado exitosamente en: {model_path}")

        # Verificar que el archivo existe
        if os.path.exists(model_path):
            print(f"Verificación: el archivo {model_path} existe")
            print(f"Tamaño del archivo: {os.path.getsize(model_path)} bytes")
        else:
            print("ERROR: El archivo no se creó correctamente")

    except Exception as e:
        print(f"Error durante el entrenamiento: {str(e)}")
        raise

if __name__ == '__main__':
    try:
        print("Iniciando script de entrenamiento...")
        train_and_save_model()
        print("Script completado exitosamente")
    except Exception as e:
        print(f"Error en el script principal: {str(e)}")