//  Copyright (C) 2005 - 2013  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//
//  Contact: info@doctor-doc.com

package util;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import enums.XPrio;

/**
 * Class to send emails. </P></P>
 * 
 * @author Pascal Steiner, Markus Fischer
 */
public class MHelper extends AbstractReadSystemConfigurations {
    
    private static final Logger LOG = LoggerFactory.getLogger(MHelper.class);
    
    private InternetAddress[] to;
    private String subject;
    private String text;
    private String replyTo;
    private String xprio = XPrio.NORMAL.getValue(); // Default
    private DataSource attachment;
    private String filename;
    
    private static final String UTF8 = "UTF-8";
    
    public MHelper(final InternetAddress[] to, final String subject, final String text) {
        this.setTo(to);
        this.setSubject(subject);
        this.setText(text);
    }
    
    public MHelper(final InternetAddress[] to, final String subject, final String text, final DataSource attachment,
            final String filename) {
        this.setTo(to);
        this.setSubject(subject);
        this.setText(text);
        this.setAttachment(attachment);
        this.setFilename(filename);
    }
    
    public MHelper(final Exception e, final String subject) {
        final InternetAddress[] errorMail = new InternetAddress[1];
        try {
            errorMail[0] = new InternetAddress(ERROR_EMAIL);
        } catch (final AddressException ex) {
            LOG.error("Please check the key 'errorEmail.email' in SystemConfiguration.properties: " + ex.toString());
        }
        this.setTo(errorMail);
        this.setSubject(subject);
        this.setText(e.toString());
    }
    
    public MHelper(final Exception e, final String subject, final String text) {
        final InternetAddress[] errorMail = new InternetAddress[1];
        try {
            errorMail[0] = new InternetAddress(ERROR_EMAIL);
        } catch (final AddressException ex) {
            LOG.error("Please check the key 'errorEmail.email' in SystemConfiguration.properties: " + ex.toString());
        }
        this.setTo(errorMail);
        this.setSubject(subject);
        this.setText(e.toString() + "\n\n" + text);
    }
    
    /**
     * Sends an email depending on the arguments specified: replyTo, xprio and
     * attachment will be set if appropriate. Contents are UTF-8 encoded.
     * </P></P>
     * 
     * @author Markus Fischer
     */
    public void send() throws Exception {
        
        validateArguments();
        
        // create properties and get the default Session
        final Session session = Session.getInstance(getProperties());
        
        // create a message
        final MimeMessage msg = getMimeMessage(session, this.getXPrio());
        
        // make TO addresses visible
        msg.setRecipients(Message.RecipientType.TO, this.getTo());
        
        // set optional replyTo address
        if (this.getReplyTo() != null) {
            final InternetAddress[] addressReplyTo = new InternetAddress[1];
            addressReplyTo[0] = new InternetAddress(this.getReplyTo());
            msg.setReplyTo(addressReplyTo);
        }
        
        // Setting the Subject
        msg.setSubject(MimeUtility.encodeText(this.getSubject(), UTF8, null));
        
        if (this.getAttachment() == null) { // email without attachment
            msg.setText(this.getText(), UTF8);
            msg.saveChanges();
        } else { // email with attachment
            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            
            // set text message
            messageBodyPart.setText(this.getText(), UTF8);
            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            
            // part two is the attachment
            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setDataHandler(new DataHandler(this.getAttachment()));
            messageBodyPart.setFileName(this.getFilename());
            multipart.addBodyPart(messageBodyPart);
            
            // put parts in message
            msg.setContent(multipart);
            msg.saveChanges();
        }
        
        // send email
        sendMessage(session, msg);
        
    }
    
    /**
     * Convenience method: allows to send an (error) email without having to
     * deal with exceptions in the calling classes.
     */
    public void sendError() {
        try {
            send();
        } catch (final Exception e) {
            LOG.error(e.toString());
        }
    }
    
    /**
     * Validates input on illegal empty or null values from constructor, before
     * sending an email.
     */
    private void validateArguments() throws IllegalArgumentException {
        
        if (this.getTo() == null || this.getTo().length == 0) {
            throw new IllegalArgumentException("No values for InternetAddress[] to");
        }
        
        if (this.getSubject() == null) {
            throw new IllegalArgumentException("Invalid subject: null");
        }
        
        if (this.getSubject() == null) {
            throw new IllegalArgumentException("Invalid mail text: null");
        }
        
        if (this.getAttachment() != null && this.getFilename() == null) {
            throw new IllegalArgumentException("Missing filename: null");
        }
    }
    
    /** Gets the system properties */
    private Properties getProperties() {
        // Get system properties
        final Properties props = System.getProperties();
        // Setup mail server
        props.setProperty("mail.smtp.host", SYSTEM_EMAIL_HOST);
        props.setProperty("mail.user", getLoadBalancedAccontname());
        props.setProperty("mail.password", SYSTEM_EMAIL_PASSWORD);
        props.setProperty("mail.smtp.auth", "true");
        return props;
    }
    
    /** Gets the MimeMessage */
    private MimeMessage getMimeMessage(final Session session, final String xprio) throws MessagingException,
            AddressException {
        // Define message
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SYSTEM_EMAIL));
        message.setSentDate(new Date()); // set date
        //        message.addHeader("charset", UTF8);
        message.setHeader("Content-Type", "text/plain; charset=utf-8");
        message.addHeader("X-Priority", xprio);
        
        return message;
    }
    
    /** Send the message to @To */
    private void sendMessage(final Session session, final Message m) throws MessagingException, NoSuchProviderException {
        
        final Transport bus = session.getTransport("smtp");
        try {
            bus.connect(SYSTEM_EMAIL_HOST, getLoadBalancedAccontname(), SYSTEM_EMAIL_PASSWORD);
            bus.sendMessage(m, this.getTo());
        } finally {
            bus.close();
        }
    }
    
    /**
     * Gets a random email from the array list of the system emails specified.
     * It returns always the same email, if only one email is specified,
     **/
    private String getLoadBalancedAccontname() {
        final int randomNumber = getRandomNumber(1, SYSTEM_EMAIL_ACCOUNTNAME.length);
        return SYSTEM_EMAIL_ACCOUNTNAME[randomNumber - 1];
        
    }
    
    /** Returns a random number between and including a minimum and maximum. */
    private int getRandomNumber(final int min, final int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }
    
    public static void main(final String[] args) {
        
    }
    
    public String getReplyTo() {
        return replyTo;
    }
    
    public void setReplyTo(final String replyTo) {
        this.replyTo = replyTo;
    }
    
    public String getXPrio() {
        return xprio;
    }
    
    public void setXPrio(final String xprio) {
        this.xprio = xprio;
    }
    
    // private to enforce constructor
    private InternetAddress[] getTo() {
        return to;
    }
    
    // private to enforce constructor
    private void setTo(final InternetAddress[] to) {
        this.to = to;
    }
    
    // private to enforce constructor
    private String getSubject() {
        return subject;
    }
    
    // private to enforce constructor
    private void setSubject(final String subject) {
        this.subject = subject;
    }
    
    // private to enforce constructor
    private String getText() {
        return text;
    }
    
    // private to enforce constructor
    private void setText(final String text) {
        this.text = text;
    }
    
    // private to enforce constructor
    private DataSource getAttachment() {
        return attachment;
    }
    
    // private to enforce constructor
    private void setAttachment(final DataSource attachment) {
        this.attachment = attachment;
    }
    
    // private to enforce constructor
    private String getFilename() {
        return filename;
    }
    
    // private to enforce constructor
    private void setFilename(final String filename) {
        this.filename = filename;
    }
    
}
