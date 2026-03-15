package server;

import mail.AutoReplyService;
import model.AutoReplyConfig;
import model.MailRequest;
import mail.MailService;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MailServer {

    public static void main(String[] args) {

        MailService mailService = new MailService();
        AutoReplyService autoReplyService = new AutoReplyService();

        try {
            ServerSocket server = new ServerSocket(5000);
            System.out.println("Mail Server running on port 5000...");

            while (true) {

                Socket socket = server.accept();
                System.out.println("Client connected.");

                ObjectInputStream in =
                        new ObjectInputStream(socket.getInputStream());

                Object obj = in.readObject();

                // Phân biệt loại request
                if (obj instanceof AutoReplyConfig config) {

                    if (config.isEnabled()) {
                        autoReplyService.stop();
                        autoReplyService.start(config);
                        System.out.println("Auto reply ENABLED.");
                    } else {
                        autoReplyService.stop();
                        System.out.println("Auto reply DISABLED.");
                    }

                } else if (obj instanceof MailRequest req) {

                    // 1. Thực hiện gửi mail đi
                    mailService.sendMultipleEmails(req);

                    // 2. Đăng ký email vào whitelist auto reply (Code mới cập nhật)
                    if (req.getExcelFile() != null && !req.getExcelFile().isEmpty()) {
                        // Trường hợp gửi từ danh sách Excel
                        mailService.readEmails(req.getExcelFile())
                                .forEach(autoReplyService::registerSentEmail);
                    } else {
                        // Trường hợp gửi 1 địa chỉ đơn lẻ
                        if (req.getTo() != null && !req.getTo().isEmpty()) {
                            autoReplyService.registerSentEmail(req.getTo());
                        }
                    }
                }

                socket.close();
                System.out.println("Request completed.\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}