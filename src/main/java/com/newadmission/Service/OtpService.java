package com.newadmission.Service;


import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
    public class OtpService {

        private final Map<String, OtpData> otpMap = new ConcurrentHashMap<>();

        public void saveOtp(String email, String otp) {
            otpMap.put(email, new OtpData(otp, LocalDateTime.now().plusMinutes(5)));
        }

        public boolean verifyOtp(String email, String otp) {
            OtpData data = otpMap.get(email);
            if (data == null || LocalDateTime.now().isAfter(data.expiry)) return false;
            boolean match = data.otp.equals(otp);
            if (match) otpMap.remove(email); // one-time use
            return match;
        }

        private record OtpData(String otp, LocalDateTime expiry) {}
    }



