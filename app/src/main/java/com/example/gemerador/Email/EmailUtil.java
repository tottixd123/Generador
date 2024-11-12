package com.example.gemerador.Email;
public class EmailUtil {
    public static String createWelcomeEmailBody(String nombre, String email, String password) {
        return "Estimado/a " + nombre + ",\n\n" +
                "¡Bienvenido/a al sistema! Su cuenta ha sido creada exitosamente.\n\n" +
                "Sus credenciales de acceso son:\n" +
                "Email: " + email + "\n" +
                "Contraseña: " + password + "\n\n" +
                "Por favor, cambie su contraseña después del primer inicio de sesión por motivos de seguridad.\n\n" +
                "Si tiene alguna pregunta o necesita ayuda, no dude en contactarnos.\n\n" +
                "Saludos cordiales,\n" +
                "El equipo de administración";
    }
}
