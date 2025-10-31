package com.newadmission.Serviceimpl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.newadmission.JWT.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdmissionBarcodeService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StaffService staffService;

    private static final String ADMISSION_BASE_URL = "https://pjsofttech.in/layeringadmissionqr";


    public byte[] generateQRCodeForAdmission(String role, String email) {
        try {
            String endpoint = switch (role.toLowerCase()) {
                case "branch" -> "/branch/getbranchcode";
                case "department" -> "/department/getbranchcode";
                case "staff" -> "/staff/getbranchcode";
                default -> throw new IllegalArgumentException("Invalid role: " + role);
            };

            String branchCode = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(endpoint).queryParam("email", email).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (branchCode == null || branchCode.isEmpty()) {
                throw new RuntimeException("Branch code not found for email: " + email);
            }

            String instituteEmail = staffService.getInstituteEmailByBranchCode(branchCode).block();
            if (instituteEmail == null || instituteEmail.isEmpty()) {
                throw new RuntimeException("Institute email not found for branch code: " + branchCode);
            }

            instituteEmail = instituteEmail.replace("instituteEmail =", "")
                    .replace("\"", "")
                    .trim();


            String encodedInstituteEmail = URLEncoder.encode(instituteEmail, StandardCharsets.UTF_8);


            String encodedBranchCode = Base64.getUrlEncoder().encodeToString(branchCode.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role.toUpperCase());
            claims.put("branchCode", encodedBranchCode);
            claims.put("email", email);

            String jwt = jwtUtil.generateTokenWithClaims("anonymous@user.com", claims, Duration.ofDays(90));

            String qrData = ADMISSION_BASE_URL + "?token=" + jwt+"&instituteEmail=" + encodedInstituteEmail;

            QRCodeWriter barcodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = barcodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate QR code for admission: " + e.getMessage(), e);
        }
    }

}
