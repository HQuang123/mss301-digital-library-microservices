package fu.edu.mss301.digilib.notification.infrastructure.mail;

import fu.edu.mss301.digilib.notification.application.service.MailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements MailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }
}
