package utils;

import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static utils.Constants.*;

public class EmailUtils {

    private EmailUtils(){}
    final static Logger logger = Logger.getLogger(EmailUtils.class);

    public static final Properties EMAIL_PROPS = RegistrationUtils.readPropertyFile("/secure/email.properties");
    public static final Properties SMTP_PROPS = RegistrationUtils.readPropertyFile("/smtp.properties");

    public static void sendToEmail(String info) {
        String to = EMAIL_PROPS.getProperty(EMAIL_TO);
        String from = EMAIL_PROPS.getProperty(EMAIL_FROM);

        Session session = Session.getInstance(SMTP_PROPS, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(from, EMAIL_PROPS.getProperty(FROM_PASSWORD));
            }
        });
        session.setDebug(false);

        try {
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("New appointment for Registration is available now");
            message.setText(info);

            Transport.send(message);
        } catch (MessagingException mex) {
            logger.error("Email was not sent. ");
        }
    }
}
