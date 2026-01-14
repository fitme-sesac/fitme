package com.example.pproject.sms;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SolapiSmsService {

    private final DefaultMessageService messageService;

    @Value("${solapi.sender}")
    private String sender; /**
     * Creates a SolapiSmsService and initializes the Solapi DefaultMessageService client.
     *
     * @param apiKey    the Solapi API key (injected from configuration)
     * @param apiSecret the Solapi API secret (injected from configuration)
     */

    public SolapiSmsService(@Value("${solapi.api-key}") String apiKey,
                            @Value("${solapi.api-secret}") String apiSecret) {
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
    }

    /**
     * Send a one-time passcode SMS (message includes a 5-minute validity notice) to the specified recipient number.
     *
     * @param toDigitsOnly recipient phone number containing digits only
     * @param code         one-time passcode to include in the SMS
     * @throws IllegalStateException if the SMS fails to send or an error occurs while sending
     */
    public void sendOtp(String toDigitsOnly, String code) {
        // SMS는 90byte(한글 45자) 제한이 있으니 문구를 짧게 유지 권장 :contentReference[oaicite:7]{index=7}
        String text = "[FIT-ME] 인증번호 " + code + " (5분)";

        Message msg = new Message();
        msg.setFrom(sender);        // 등록 발신번호 필수 :contentReference[oaicite:8]{index=8}
        msg.setTo(toDigitsOnly);    // 수신번호(회원 입력)
        msg.setText(text);

        try {
            messageService.send(msg);
        } catch (SolapiMessageNotReceivedException e) {
            // 부분 실패/실패 리스트가 있을 수 있음 → 운영에서는 e.getFailedMessageList() 로 로깅 권장
            throw new IllegalStateException("SMS 발송 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("SMS 발송 오류: " + e.getMessage(), e);
        }
    }
}