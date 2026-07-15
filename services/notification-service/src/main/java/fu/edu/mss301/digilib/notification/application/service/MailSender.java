package fu.edu.mss301.digilib.notification.application.service;

public interface MailSender {

    void send(String toEmail, String subject, String body);
}
