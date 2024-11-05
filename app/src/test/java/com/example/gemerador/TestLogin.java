package com.example.gemerador;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.text.Editable;
import android.text.InputFilter;
import android.widget.EditText;


import com.example.gemerador.login.Login;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class TestLogin {

    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private Task<AuthResult> mockAuthTask;

    private Login loginActivity;
    private EditText mockEmailEditText;
    private EditText mockPasswordEditText;

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = Login.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(loginActivity, value);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Crear la actividad usando Robolectric
        loginActivity = Robolectric.buildActivity(Login.class)
                .create()
                .resume()
                .get();

        // Crear mocks para los EditText
        mockEmailEditText = mock(EditText.class);
        mockPasswordEditText = mock(EditText.class);

        // Configurar el comportamiento de los EditText mock
        when(mockEmailEditText.getText()).thenReturn(new MockEditable("test@example.com"));
        when(mockPasswordEditText.getText()).thenReturn(new MockEditable("password123"));

        // Inyectar los mocks usando reflexión
        setPrivateField("editTextUsuario", mockEmailEditText);
        setPrivateField("editTextContrasena", mockPasswordEditText);
        setPrivateField("mAuth", mockAuth);

        // Configuración básica del mock de Firebase
        when(mockAuth.signInWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockAuthTask);
    }

    @Test
    public void testIniciarSesion_Success() throws Exception {
        // Configurar comportamiento exitoso
        when(mockAuthTask.isSuccessful()).thenReturn(true);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn("test-uid");

        // Ejecutar el método a probar
        loginActivity.iniciarSesion();

        // Verificar que se llamó al método de autenticación con los parámetros correctos
        verify(mockAuth).signInWithEmailAndPassword("test@example.com", "password123");
    }

    @Test
    public void testIniciarSesion_EmptyFields() throws Exception {
        // Configurar campos vacíos
        when(mockEmailEditText.getText()).thenReturn(new MockEditable(""));
        when(mockPasswordEditText.getText()).thenReturn(new MockEditable(""));

        // Ejecutar el método
        loginActivity.iniciarSesion();

        // Verificar que no se llamó a Firebase Auth
        verify(mockAuth, never()).signInWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void testIniciarSesion_Failure() throws Exception {
        // Configurar fallo de autenticación
        when(mockAuthTask.isSuccessful()).thenReturn(false);
        when(mockAuthTask.getException()).thenReturn(new Exception("Authentication failed"));

        // Ejecutar el método
        loginActivity.iniciarSesion();

        // Verificar que se llamó al método de autenticación
        verify(mockAuth).signInWithEmailAndPassword("test@example.com", "password123");
    }

    // Clase auxiliar para simular el Editable de los EditText
    private static class MockEditable implements android.text.Editable {
        private String text;

        MockEditable(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }

        @Override
        public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
            text.getChars(srcBegin, srcEnd, dst, dstBegin);
        }

        @Override
        public void clear() {
        }

        @Override
        public void clearSpans() {
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int i) {
            return text.charAt(i);
        }

        @Override
        public Editable replace(int st, int en, CharSequence source, int start, int end) {
            return this;
        }

        @Override
        public Editable replace(int st, int en, CharSequence text) {
            return this;
        }

        @Override
        public Editable insert(int where, CharSequence text, int start, int end) {
            return this;
        }

        @Override
        public Editable insert(int where, CharSequence text) {
            return this;
        }

        @Override
        public Editable delete(int st, int en) {
            return this;
        }

        @Override
        public void setFilters(InputFilter[] filters) {
        }

        @Override
        public InputFilter[] getFilters() {
            return new InputFilter[0];
        }

        // Métodos append requeridos
        @Override
        public Editable append(CharSequence text) {
            return this;
        }

        @Override
        public Editable append(CharSequence text, int start, int end) {
            return this;
        }

        @Override
        public Editable append(char text) {
            return this;
        }

        // Métodos setSpan requeridos
        @Override
        public void setSpan(Object what, int start, int end, int flags) {
        }

        @Override
        public void removeSpan(Object what) {
        }

        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return null;
        }

        @Override
        public int getSpanStart(Object tag) {
            return 0;
        }

        @Override
        public int getSpanEnd(Object tag) {
            return 0;
        }

        @Override
        public int getSpanFlags(Object tag) {
            return 0;
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            return 0;
        }
    }
}
