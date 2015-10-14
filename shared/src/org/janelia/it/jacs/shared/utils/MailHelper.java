
package org.janelia.it.jacs.shared.utils;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


/**
 * Class to facilitate email messaging from the code
 * Code found from http://www.kodejava.org/examples/240.html
 */
public class MailHelper {
    // NOTE:  Trying to run this from IntelliJ on a Windows box does not work.  Our machines are probably not allowed to
    // send mail if it isn't from outlook.  A permission error is thrown.  It does work from Linux.
    public void sendEmail(String from, String to, String subject, String bodyText) {
        // For attachments if we ever need
//        String filename = "message.pdf";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", SystemConfigurationProperties.getString("Mail.Server"));
        properties.put("mail.smtp.port", SystemConfigurationProperties.getString("Mail.Port"));
        Session session = Session.getDefaultInstance(properties, null);

        try {
            //
            // Message is a mail message to be send through the Transport object.
            // In the Message object we set the sender address and the recipent
            // address. Both of this address is a type of InternetAddress. For
            // the recipient address we can also set the type of recipient, the
            // value can be TO, CC or BCC. In the next two lines we set the email
            // subject and the content text.
            //
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("[JW] " + subject);
            message.setText(bodyText);

            //
            // Send the message to the recipient.
            //
            Transport.send(message);
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}