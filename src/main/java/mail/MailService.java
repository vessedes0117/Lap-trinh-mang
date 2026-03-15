package mail;

import model.MailRequest;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MailService {

    // MAIL HỆ THỐNG
    private static final String SYSTEM_EMAIL = "mangoisme03@gmail.com";
    private static final String SYSTEM_PASSWORD = "ecejffzypkzvymwn";

    public static void sendMail(String to,
                                String subject,
                                String content,
                                String filePath)
            throws Exception {

        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        SYSTEM_EMAIL,
                        SYSTEM_PASSWORD
                );
            }
        });

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(SYSTEM_EMAIL));

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(to)
        );

        message.setSubject(subject);

        // HEADER giảm spam
        message.setHeader("X-Mailer", "Java Mailer");
        message.setHeader("Content-Type", "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();

        // GỬI HTML MAIL
        textPart.setContent(
                "<h3>"+subject+"</h3>" +
                        "<p>"+content+"</p>",
                "text/html; charset=utf-8"
        );

        multipart.addBodyPart(textPart);

        // ATTACHMENT
        if (filePath != null && !filePath.isEmpty()) {

            MimeBodyPart attachPart = new MimeBodyPart();

            attachPart.attachFile(new File(filePath));

            multipart.addBodyPart(attachPart);
        }

        message.setContent(multipart);

        Transport.send(message);
    }

    // Đọc email từ Excel
    public List<String> readEmails(String filePath) {

        List<String> emails = new ArrayList<>();

        try {

            FileInputStream fis = new FileInputStream(filePath);

            Workbook workbook = new XSSFWorkbook(fis);

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue;

                Cell cell = row.getCell(0);

                if (cell != null) {

                    String email = cell.getStringCellValue().trim();

                    if (!email.isEmpty()) {

                        emails.add(email);
                    }
                }
            }

            workbook.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return emails;
    }

    // Gửi nhiều mail
    public void sendMultipleEmails(MailRequest req) {

        List<String> emails = new ArrayList<>();

        if (req.getExcelFile() != null && !req.getExcelFile().isEmpty()) {

            emails = readEmails(req.getExcelFile());

        } else {

            emails.add(req.getTo());
        }

        System.out.println("Total emails: " + emails.size());

        for (String email : emails) {

            try {

                sendMail(
                        email,
                        req.getSubject(),
                        req.getContent(),
                        req.getAttachmentPath()
                );

                System.out.println("Sent to: " + email);

                // delay tránh Gmail block
                Thread.sleep(3000);

            } catch (Exception e) {

                System.out.println("Failed: " + email);

                e.printStackTrace();
            }
        }
    }
}
