package com.newadmission.Serviceimpl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Repository.AdmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3Service {

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final String region;
    private final AdmissionRepository admissionFormRepository;

    @Autowired
    public S3Service(@Value("${aws.s3.bucket-name}") String bucketName,
                     @Value("${aws.access-key}") String accessKey,
                     @Value("${aws.secret-key}") String secretKey,
                     @Value("${aws.region}") String region,
                     AdmissionRepository admissionFormRepository) {
        this.bucketName = bucketName;
        this.region = region;
        this.admissionFormRepository = admissionFormRepository;

        if (accessKey == null || secretKey == null || bucketName == null || region == null) {
            throw new IllegalArgumentException("AWS credentials or configuration is missing.");
        }

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .enablePathStyleAccess()
                .build();

    }

    public String uploadFileToDocs(MultipartFile file, String branchCode, String systemName) throws IOException {
        System.out.println("----- Uploading File to S3 -----");
        System.out.println("File Name: " + file.getOriginalFilename());
        System.out.println("File Size: " + file.getSize());
        System.out.println("Branch Code: " + branchCode);
        System.out.println("System Name: " + systemName);
        System.out.println("Bucket: " + bucketName);
        System.out.println("Region: " + region);

        byte[] fileBytes = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileBytes.length);
        metadata.setContentType(file.getContentType());

        try {
            String key = branchCode + "/admission-sys/docs/" + file.getOriginalFilename();
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);

            System.out.println("Putting object to S3...");
            s3Client.putObject(request);
            System.out.println("Upload completed!");

            return s3Client.getUrl(bucketName, key).toString();
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            throw new RuntimeException("Error uploading file to S3", e);
        } catch (SdkClientException e) {
            e.printStackTrace(); // <== this is key!
            throw new RuntimeException("AWS SDK client exception", e);
        }
    }

    public String uploadFileToAttendanceFaces(MultipartFile file, String branchCode, String systemName, Long admissionId) throws IOException {
        System.out.println("----- Uploading Image to Attendance Path -----");

        byte[] fileBytes = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileBytes.length);
        metadata.setContentType(file.getContentType());

        String fileName = file.getOriginalFilename();
        String key = branchCode + "/" + systemName + "/attendance_faces/" + admissionId + "/" + fileName;

        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            s3Client.putObject(request);
            return s3Client.getUrl(bucketName, key).toString();
        } catch (SdkClientException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload to attendance_faces folder", e);
        }
    }


    public String copyImageToAttendanceFolderWithRollNoFilename(Long admissionFormId, Long classroomId, Integer rollNo, String systemName) {
        AdmissionForm admissionForm = admissionFormRepository.findById(admissionFormId)
                .orElseThrow(() -> new IllegalArgumentException("AdmissionForm not found"));

        String branchCode = admissionForm.getBranchCode();
        if (branchCode == null || branchCode.isEmpty()) {
            throw new IllegalArgumentException("Branch code not found");
        }

        if (systemName == null || systemName.isEmpty()) {
            systemName = "default-sys";
        }

        String currentImageUrl = admissionForm.getStudentImage();
        if (currentImageUrl == null || currentImageUrl.isEmpty()) {
            throw new IllegalArgumentException("Student image URL is empty");
        }

        try {
            // ✅ Use URI to handle both region and non-region URLs safely
            URI uri = URI.create(currentImageUrl);
            String path = uri.getPath(); // e.g. "/attendace-docs/BCH584/admission-sys/docs/photo.jpg"

            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Invalid S3 URL format: " + currentImageUrl);
            }

            // Remove leading "/" if present
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // ✅ Verify that the path starts with your S3 bucket name
            if (!path.startsWith(bucketName + "/")) {
                throw new IllegalArgumentException("S3 URL does not contain expected bucket name: " + currentImageUrl);
            }

            // Extract the object key (everything after "<bucketName>/")
            String sourceKey = path.substring(bucketName.length() + 1);

            // Decode any URL-encoded characters (spaces, etc.)
            sourceKey = URLDecoder.decode(sourceKey, StandardCharsets.UTF_8);

            // ✅ Extract file extension
            String originalFileName = sourceKey.substring(sourceKey.lastIndexOf("/") + 1);
            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFileName.substring(dotIndex);
            }

            // ✅ Build the new key (destination path)
            String destKey = branchCode + "/" + systemName + "/attendance_faces/" + classroomId + "/" + rollNo + extension;

            // ✅ Debug log (optional)
            System.out.println("SOURCE KEY: " + sourceKey);
            System.out.println("DESTINATION KEY: " + destKey);

            // ✅ Verify that source exists before copying
            if (!s3Client.doesObjectExist(bucketName, sourceKey)) {
                throw new RuntimeException("Source file does not exist in S3 at key: " + sourceKey);
            }

            // ✅ Perform the copy
            s3Client.copyObject(bucketName, sourceKey, bucketName, destKey);

            // ✅ Generate the new public URL
            String newUrl = s3Client.getUrl(bucketName, destKey).toString();

            // ✅ Update database with new image URL
            admissionForm.setStudentImage(newUrl);
            admissionFormRepository.save(admissionForm);

            return newUrl;

        } catch (IllegalArgumentException e) {
            throw e; // rethrow expected validation errors
        } catch (Exception e) {
            // ✅ Catch-all fallback for unexpected errors
            throw new RuntimeException("Error copying image to attendance folder: " + e.getMessage(), e);
        }
    }



}
