package com.example.gemerador.Email;

public class EmailConfig {
    // Configuración para Outlook/Hotmail
    public static final String SMTP_HOST = "smtp-mail.outlook.com";
    public static final String SMTP_PORT = "587";
    public static final String EMAIL_FROM = "luis92134@hotmail.com"; // Reemplaza con tu correo
    public static final String EMAIL_PASSWORD = "tvibooxolnngsdm"; // Reemplaza con tu contraseña

    // Asunto y mensajes predeterminados
    public static final String WELCOME_SUBJECT = "Bienvenido - Credenciales de acceso";

    private EmailConfig() {
        // Constructor privado para evitar instanciación
    }
}
