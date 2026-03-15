package mail;

import model.AutoReplyConfig;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;
import java.util.*;
import java.util.concurrent.*;

public class AutoReplyService {

    private static final String SYSTEM_EMAIL = "mangoisme03@gmail.com";
    private static final String SYSTEM_PASSWORD = "ecejffzypkzvymwn";

    private ScheduledExecutorService scheduler;
    private AutoReplyConfig config;

    // Danh sách email app đã gửi → chỉ reply những người này
    private final Set<String> sentToEmails = new HashSet<>();

    public void start(AutoReplyConfig config) {
        this.config = config;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Cứ 30 giây quét inbox 1 lần
        scheduler.scheduleAtFixedRate(
                this::checkAndReply, 0, 30, TimeUnit.SECONDS
        );
        System.out.println("Auto Reply Service started.");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            System.out.println("Auto Reply Service stopped.");
        }
    }

    public void updateConfig(AutoReplyConfig config) {
        this.config = config;
    }

    // Gọi hàm này mỗi khi app gửi mail đi để đăng ký vào danh sách chờ reply
    public void registerSentEmail(String email) {
        sentToEmails.add(email.trim().toLowerCase());
        System.out.println("Registered for auto-reply: " + email);
    }

    private void checkAndReply() {
        if (config == null || !config.isEnabled()) return;

        // Nếu chưa gửi mail cho ai thì không cần check inbox làm gì cho tốn tài nguyên
        if (sentToEmails.isEmpty()) {
            System.out.println("No registered emails to reply to.");
            return;
        }

        System.out.println("Checking inbox... Watching: " + sentToEmails);

        try {
            Properties props = new Properties();
            props.put("mail.imap.host", "imap.gmail.com");
            props.put("mail.imap.port", "993");
            props.put("mail.imap.ssl.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
                }
            });

            Store store = session.getStore("imap");
            store.connect("imap.gmail.com", SYSTEM_EMAIL, SYSTEM_PASSWORD);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(
                    new FlagTerm(new Flags(Flags.Flag.SEEN), false)
            );

            System.out.println("Unseen emails found: " + messages.length);

            // Thời điểm 1 giờ trước (Chỉ xử lý mail rất mới)
            Date since = new Date(System.currentTimeMillis() - 60 * 60 * 1000L);

            for (Message msg : messages) {

                // Bỏ qua mail cũ hơn 1 giờ
                if (msg.getReceivedDate() != null && msg.getReceivedDate().before(since)) {
                    continue;
                }

                String from = InternetAddress.toString(msg.getFrom());
                String fromEmail = extractEmail(from);

                // Đánh dấu đọc dù có reply hay không để lần quét sau bỏ qua
                msg.setFlag(Flags.Flag.SEEN, true);

                // Quan Trọng: Chỉ reply nếu người gửi nằm trong danh sách app đã gửi tới
                if (!sentToEmails.contains(fromEmail)) {
                    System.out.println("Skipped (not in sent list): " + fromEmail);
                    continue;
                }

                sendReply(msg, config.getReplyTemplate());
                System.out.println("Replied to: " + from);

                Thread.sleep(2000); // Tránh bị Google block do gửi quá nhanh
            }

            inbox.close(false);
            store.close();

        } catch (Exception e) {
            System.out.println("Auto reply error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper: Tách địa chỉ email thuần từ chuỗi (VD: "Nguyen Van A <nguyenvana@gmail.com>" -> "nguyenvana@gmail.com")
    private String extractEmail(String from) {
        try {
            InternetAddress[] addrs = InternetAddress.parse(from);
            if (addrs.length > 0) {
                return addrs[0].getAddress().trim().toLowerCase();
            }
        } catch (Exception e) {
            // Fallback nếu lỗi parse
        }
        return from.trim().toLowerCase();
    }

    private void sendReply(Message original, String template) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
            }
        });

        Message reply = new MimeMessage(session);
        reply.setRecipients(Message.RecipientType.TO, original.getFrom());

        String subject = original.getSubject();
        if (subject == null) subject = "";
        if (!subject.startsWith("Re:")) subject = "Re: " + subject;
        reply.setSubject(subject);

        reply.setFrom(new InternetAddress(SYSTEM_EMAIL));

        String messageId = ((MimeMessage) original).getMessageID();
        if (messageId != null) {
            reply.setHeader("In-Reply-To", messageId);
            reply.setHeader("References", messageId);
        }

        reply.setContent(template, "text/html; charset=utf-8");
        Transport.send(reply);
    }
}