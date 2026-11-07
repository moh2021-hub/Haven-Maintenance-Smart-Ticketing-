package com.apartment.maintenance.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves a maintenance photo and returns the URL to store in the DB.
 *
 * aws.s3.enabled = false  -> saves into the local ./uploads folder (works with zero AWS setup)
 * aws.s3.enabled = true   -> uploads to your S3 bucket
 *
 * Nothing else in the app changes, so you can build the whole project locally
 * and only turn S3 on when you deploy.
 */
@Service
public class PhotoStorageService {

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (s3Enabled) {
            // Credentials come from the default chain:
            // env vars, ~/.aws/credentials, or the Elastic Beanstalk EC2 instance role.
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is empty");
        }

        String original = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String fileName = UUID.randomUUID() + extension;

        return s3Enabled ? saveToS3(file, fileName) : saveToDisk(file, fileName);
    }

    private String saveToS3(MultipartFile file, String fileName) {
        String key = "maintenance-photos/" + fileName;
        try (InputStream in = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not upload photo to S3", e);
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String saveToDisk(MultipartFile file, String fileName) {
        try {
            Path folder = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(folder);
            Path target = folder.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save photo to disk", e);
        }
        // Served by WebConfig at http://localhost:8080/photos/<fileName>
        return "/photos/" + fileName;
    }
}
