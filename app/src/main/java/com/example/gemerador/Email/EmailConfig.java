package com.example.gemerador.Email;

public class EmailConfig {
    // Configuración para Gmail
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";

    // Reemplaza con tu correo de Gmail y contraseña de aplicación
    public static final String EMAIL_FROM = "j8455807@gmail.com";
    public static final String EMAIL_PASSWORD = "t f s y a s c e u p i r e m m m"; // Reemplaza con contraseña generada por apliacaion

    // Asunto y mensajes predeterminados
    public static final String WELCOME_SUBJECT = "Bienvenido - Credenciales de acceso";

    private EmailConfig() {
        // Constructor privado para evitar instanciación
    }
}
