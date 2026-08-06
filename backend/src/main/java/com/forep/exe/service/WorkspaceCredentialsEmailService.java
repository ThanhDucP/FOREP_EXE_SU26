package com.forep.exe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sends the initial, one-time workspace owner credentials after activation. */
@Service
public class WorkspaceCredentialsEmailService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceCredentialsEmailService.class);

    private final JavaMailSender mailSender;
    private final String sender;
    private final String senderName;

    public WorkspaceCredentialsEmailService(JavaMailSender mailSender,
                                            @Value("${spring.mail.username:}") String sender,
                                            @Value("${forep.mail.from-name:FOREP EXE}") String senderName) {
        this.mailSender = mailSender;
        this.sender = sender == null ? "" : sender.trim();
        this.senderName = senderName == null || senderName.isBlank() ? "FOREP EXE" : senderName.trim();
    }

    public void sendInitialCredentials(String recipient, String username, String temporaryPassword,
                                       String workspaceName) {
        if (isBlank(sender) || isBlank(recipient) || isBlank(username) || isBlank(temporaryPassword)) {
            log.warn("Initial credentials email was skipped because mail is not configured.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipient);
            message.setSubject("Tài khoản FOREP EXE của bạn đã sẵn sàng");
            message.setText("Xin chào,\n\n"
                    + "Thanh toán cho không gian làm việc \"" + workspaceName + "\" đã được xác nhận.\n\n"
                    + "Email đăng nhập: " + recipient + "\n"
                    + "Tên đăng nhập: " + username + "\n"
                    + "Mật khẩu tạm thời: " + temporaryPassword + "\n\n"
                    + "Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đăng nhập đầu tiên. "
                    + "Không chia sẻ email này với người khác.\n\n"
                    + senderName);
            mailSender.send(message);
        } catch (Exception exception) {
            // Payment activation must remain successful even if the SMTP provider is temporarily unavailable.
            log.error("Could not send initial workspace credentials email.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
