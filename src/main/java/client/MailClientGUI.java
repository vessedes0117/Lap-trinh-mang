package client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import model.MailRequest;
import model.AutoReplyConfig; // Import thêm model cấu hình auto reply

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.File;

public class MailClientGUI extends Application {

    private String filePath = "";
    private String excelPath = "";

    @Override
    public void start(Stage stage) {

        Label title = new Label("AUTO MAILER");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold;");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        TextField txtTo = new TextField();
        txtTo.setPromptText("Email người nhận");

        TextField txtSubject = new TextField();
        txtSubject.setPromptText("Subject");

        TextArea txtContent = new TextArea();
        txtContent.setPromptText("Nội dung...");
        txtContent.setPrefHeight(150);

        // Attachment
        Label fileLabel = new Label("Chưa có file được chọn");
        Button btnChoose = new Button("Chọn file");

        FileChooser chooser = new FileChooser();

        btnChoose.setOnAction(e -> {
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                filePath = file.getAbsolutePath();
                fileLabel.setText(file.getName());
            }
        });

        // Excel
        Button btnExcel = new Button("Load Email List");
        Label excelLabel = new Label("No file");

        FileChooser excelChooser = new FileChooser();
        excelChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel File", "*.xlsx")
        );

        btnExcel.setOnAction(e -> {
            File file = excelChooser.showOpenDialog(stage);
            if (file != null) {
                excelPath = file.getAbsolutePath();
                excelLabel.setText(file.getName());
                txtTo.setDisable(true); // tắt nhập email nếu dùng excel
            }
        });

        Button btnSend = new Button("SEND EMAIL");
        btnSend.setStyle(
                "-fx-background-color:#2196F3;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14px;"
        );

        // Phải khởi tạo status ở đây để nút Gửi và nút Auto Reply đều dùng được
        Label status = new Label("Status: Ready");

        btnSend.setOnAction(e -> {
            try {
                Socket socket = new Socket("localhost", 5000);

                MailRequest req = new MailRequest(
                        txtTo.getText(),
                        txtSubject.getText(),
                        txtContent.getText(),
                        filePath
                );
                req.setExcelFile(excelPath);

                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream());

                out.writeObject(req);
                socket.close();

                status.setText("Gửi mail thành công");
            } catch (Exception ex) {
                status.setText("Lỗi gửi mail");
                ex.printStackTrace();
            }
        });

        // ===== AUTO REPLY SECTION =====
        Label autoReplyTitle = new Label("Auto Reply");
        autoReplyTitle.setStyle("-fx-font-weight:bold;");

        CheckBox chkAutoReply = new CheckBox("Bật Auto Reply");

        TextArea txtReplyTemplate = new TextArea();
        txtReplyTemplate.setPromptText("Nội dung tự động reply...");
        txtReplyTemplate.setPrefHeight(80);
        txtReplyTemplate.setDisable(true); // mặc định disabled

        // Khi tick checkbox → enable/disable textarea
        chkAutoReply.setOnAction(e -> {
            txtReplyTemplate.setDisable(!chkAutoReply.isSelected());
        });

        Button btnApplyAutoReply = new Button("Áp dụng Auto Reply");
        btnApplyAutoReply.setOnAction(e -> {
            try {
                Socket socket = new Socket("localhost", 5000);

                AutoReplyConfig config = new AutoReplyConfig(
                        chkAutoReply.isSelected(),
                        txtReplyTemplate.getText()
                );

                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(config);
                socket.close();

                status.setText(chkAutoReply.isSelected()
                        ? "Auto Reply đã bật"
                        : "Auto Reply đã tắt");

            } catch (Exception ex) {
                status.setText("Lỗi kết nối server");
                ex.printStackTrace();
            }
        });

        VBox autoReplyBox = new VBox(8,
                autoReplyTitle,
                chkAutoReply,
                txtReplyTemplate,
                btnApplyAutoReply
        );
        // ==============================================

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        form.add(new Label("To Email:"), 0, 0);
        form.add(txtTo, 1, 0);
        form.add(new Label("Subject:"), 0, 1);
        form.add(txtSubject, 1, 1);

        VBox contentBox = new VBox(10, new Label("Content:"), txtContent);
        HBox fileBox = new HBox(10, btnChoose, fileLabel);
        HBox excelBox = new HBox(10, btnExcel, excelLabel);

        VBox root = new VBox(20,
                title,
                form,
                contentBox,
                fileBox,
                excelBox,
                autoReplyBox,   // <-- Đã thêm autoReplyBox vào giao diện chính
                btnSend,
                status
        );

        root.setPadding(new Insets(20));

        // Tăng chiều cao (height) từ 500 lên 750 để chứa đủ cụm Auto Reply mới
        Scene scene = new Scene(root, 450, 750);

        stage.setTitle("Auto Mailer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}