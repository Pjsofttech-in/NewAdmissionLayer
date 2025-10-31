package com.newadmission.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendAdmissionConfirmation(String toEmail, String studentName, String courseName, String startDate) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(toEmail);
            helper.setSubject("🎓 Admission Confirmation for " + courseName); // ✅ Dynamic subject

            String htmlContent = """
                        <html>
                        <body style="font-family: Arial, sans-serif;">
                            <h2>Hello %s,</h2>
                            <p>We are happy to confirm your admission to the course: <strong>%s</strong>.</p>
                            <p><b>Admission Date:</b> %s</p>
                            <p style="color:green;"><strong>Your admission is successfully completed.</strong></p>
                            <br>
                            <p>Thank you for choosing <strong>PJSOFTTECH</strong>.</p>
                            <hr>
                            <p style="font-size: 12px;">Regards,<br>PJ SoftTech Team</p>
                        </body>
                        </html>
                    """.formatted(studentName, courseName, startDate);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send admission confirmation email", e);
        }
    }


    public void sendCustomEmail(String toEmail, String subject, String messageContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(messageContent, true); // true = HTML email

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }

    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "Your OTP for Password Reset";
        String message = "Dear User,\n\n" +
                "Your OTP for password reset is: " + otp + "\n" +
                "This OTP is valid for 5 minutes and will expire after one use.\n\n" +
                "Thank you,\nSupport Team";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
}