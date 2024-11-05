package com.example.gemerador;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;

import com.example.gemerador.Crear_Ti.Crear_nuevo_ti;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class Crear_nuevo_tiTest {
    private static final String PREFS_NAME = "TicketPrefs";
    private static final String TICKET_COUNTER_KEY = "ticketCounter";

    private Crear_nuevo_ti crearNuevoTiActivity;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private TextView ticketCounterTextView;

    @Mock
    private Context context;

    @Mock
    private Spinner problemSpinner;

    @Mock
    private Spinner areaSpinner;

    @Mock
    private Spinner prioritySpinner;

    @Mock
    private EditText problemDetailEditText;

    @Mock
    private Editable editable;

    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Create spy of activity
        crearNuevoTiActivity = spy(new Crear_nuevo_ti());

        // Setup mock behavior for SharedPreferences
        when(context.getSharedPreferences(eq(PREFS_NAME), eq(Context.MODE_PRIVATE)))
                .thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(sharedPreferences.getInt(eq(TICKET_COUNTER_KEY), anyInt())).thenReturn(0);
        when(editor.putInt(eq(TICKET_COUNTER_KEY), anyInt())).thenReturn(editor);

        // Setup Spinners mock behavior
        when(problemSpinner.getSelectedItem()).thenReturn("Test Problem");
        when(areaSpinner.getSelectedItem()).thenReturn("Test Area");
        when(prioritySpinner.getSelectedItem()).thenReturn("Alta");
        // Setup EditText mock behavior
        when(problemDetailEditText.getText()).thenReturn(editable);
        when(editable.toString()).thenReturn("Test Details");

        // Inject mocks into activity
        setPrivateField(crearNuevoTiActivity, "sharedPreferences", sharedPreferences);
        setPrivateField(crearNuevoTiActivity, "ticketCounterTextView", ticketCounterTextView);
        setPrivateField(crearNuevoTiActivity, "problemSpinner", problemSpinner);
        setPrivateField(crearNuevoTiActivity, "areaSpinner", areaSpinner);
        setPrivateField(crearNuevoTiActivity, "prioritySpinner", prioritySpinner);
        setPrivateField(crearNuevoTiActivity, "problemDetailEditText", problemDetailEditText);
    }

    @Test
    public void testInicializacionDeContador() {
        // Verify initial counter value is 0
        int ticketCounter = sharedPreferences.getInt(TICKET_COUNTER_KEY, 0);
        assertEquals(0, ticketCounter);
    }

    @Test
    public void testSendTicket() throws Exception {
        // Setup mock behavior for editor
        doReturn(editor).when(editor).putInt(eq(TICKET_COUNTER_KEY), anyInt());
        doNothing().when(editor).apply();

        // Mock FirebaseAuth y FirebaseUser
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getEmail()).thenReturn("test@example.com");
        setPrivateField(crearNuevoTiActivity, "mAuth", mockAuth);

        // Get private sendTicket method using reflection
        Method sendTicketMethod = Crear_nuevo_ti.class.getDeclaredMethod("sendTicket");
        sendTicketMethod.setAccessible(true);

        // Call sendTicket method
        sendTicketMethod.invoke(crearNuevoTiActivity);

        // Verify interactions
        verify(editor).putInt(eq(TICKET_COUNTER_KEY), eq(1));
        verify(editor).apply();
        verify(problemSpinner).getSelectedItem();
        verify(areaSpinner).getSelectedItem();
        verify(prioritySpinner).getSelectedItem();
        verify(problemDetailEditText).getText();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
