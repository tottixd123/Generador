package com.example.gemerador.Email;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JavaMailAPI  {
    private static final String TAG = "JavaMailAPI";

    private final Context context;
    private final String email;
    private final String subject;
    private final String message;
    private final OnEmailSentListener listener;

    public interface OnEmailSentListener {
        void onEmailSent(boolean success, String message);
    }

    public JavaMailAPI(Context context, String email, String subject, String message, OnEmailSentListener listener) {
        this.context = context;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.listener = listener;
    }

    public void sendEmail() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Properties properties = new Properties();
                properties.put("mail.smtp.host", EmailConfig.SMTP_HOST);
                properties.put("mail.smtp.port", EmailConfig.SMTP_PORT);
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_PASSWORD);
                    }
                });
                MimeMessage mimeMessage = new MimeMessage(session);
                mimeMessage.setFrom(new InternetAddress(EmailConfig.EMAIL_FROM));
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(message);

                Transport.send(mimeMessage);

                handler.post(() -> {
                    if (listener != null) {
                        listener.onEmailSent(true, "Correo enviado exitosamente");
                    }
                });

            } catch (MessagingException e) {
                Log.e(TAG, "Error sending email: " + e.getMessage());
                handler.post(() -> {
                    if (listener != null) {
                        listener.onEmailSent(false, e.getMessage());
                    }
                });
            }
        });

        executor.shutdown();
    }
}
