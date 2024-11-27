import pandas as pd
import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
import os

def load_and_preprocess_data():
    data = pd.read_csv('tickets_data.csv', sep=';', encoding='utf-8')

    # Normalizar nombres de columnas
    data.columns = ['Area_Problema', 'Prioridad', 'Tiempo_resolucion', 'Trabajador', 'Estatus']

    # Normalizar prioridades
    priority_map = {
        'Alta': 'HIGH',
        'Media': 'NORMAL',
        'Baja': 'LOW'
    }
    data['Prioridad'] = data['Prioridad'].map(priority_map)

    # Codificar variables categóricas
    le_area = LabelEncoder()
    le_prioridad = LabelEncoder()
    le_status = LabelEncoder()

    data['Area_encoded'] = le_area.fit_transform(data['Area_Problema'])
    data['Prioridad_encoded'] = le_prioridad.fit_transform(data['Prioridad'])
    data['Estatus_encoded'] = le_status.fit_transform(data['Estatus'])

    # Normalizar tiempo de resolución
    data['Tiempo_normalizado'] = (data['Tiempo_resolucion'] - data['Tiempo_resolucion'].mean()) / data['Tiempo_resolucion'].std()

    # Preparar features
    X = np.column_stack((
        data['Area_encoded'],
        data['Prioridad_encoded'],
        data['Tiempo_normalizado']
    ))

    y = data['Trabajador'] - 1  # Ajustar a base 0

    return train_test_split(X, y, test_size=0.2, random_state=42)

def create_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(3,)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])

    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=['accuracy']
    )

    return model

def train_and_save_model():
    # Crear directorio si no existe
    model_path = os.path.join('app', 'src', 'main', 'assets', 'ticket_model.tflite')
    os.makedirs(os.path.dirname(model_path), exist_ok=True)

    # Cargar y preparar datos
    X_train, X_test, y_train, y_test = load_and_preprocess_data()

    # Crear y entrenar modelo
    model = create_model()

    # Añadir early stopping
    early_stopping = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=5,
        restore_best_weights=True
    )

    # Entrenar modelo
    history = model.fit(
        X_train, y_train,
        epochs=100,
        batch_size=32,
        validation_split=0.2,
        callbacks=[early_stopping],
        verbose=1
    )

    # Convertir a TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    # Guardar modelo
    with open(model_path, 'wb') as f:
        f.write(tflite_model)

    print(f"Modelo guardado en: {model_path}")

    # Evaluar modelo
    loss, accuracy = model.evaluate(X_test, y_test)
    print(f"Test accuracy: {accuracy:.4f}")

if __name__ == '__main__':
    train_and_save_model()