
package freego

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun sendMail(subject: String, text: String) {
    //Setting up configurations for the email connection to the Google SMTP server using TLS
    val props = Properties()
    props.put("mail.smtp.host", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", "smtp.exmail.qq.com")
    props.put("mail.smtp.port", "587")
    props.put("mail.smtp.auth", "true")
    //Establishing a session with required user details
    val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(mail_from, mail_pass)
        }
    })
    try {
        //Creating a Message object to set the email content
        val msg = MimeMessage(session)

        /*Parsing the String with defualt delimiter as a comma by marking the boolean as true and storing the email
        addresses in an array of InternetAddress objects*/
        val address = InternetAddress.parse(mail_to, true)
        //Setting the recepients from the address variable
        msg.setFrom(InternetAddress(mail_from, "建行支付平台"))
        msg.setRecipients(Message.RecipientType.TO, address)
        val timeStamp = SimpleDateFormat("yyyy-mm-dd hh:mm:ss").format(Date())
        msg.setSubject("$subject($timeStamp)")
        msg.setSentDate(Date())
        // msg.setText(text)
        msg.setText(text, "utf-8", "html");
        // msg.setContent(text, "text/html; charset=utf-8");
        msg.setHeader("XPriority", "1")
        Transport.send(msg)
        println("Mail has been sent successfully")
    } catch (mex: MessagingException) {
        println("Unable to send an email" + mex)
    }
}