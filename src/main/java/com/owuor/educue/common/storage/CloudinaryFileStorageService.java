package com.owuor.educue.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
@ConditionalOnProperty(name="app.storage.provider",havingValue="cloudinary",matchIfMissing=true)
public class CloudinaryFileStorageService implements FileStorageService {
    private final RestClient restClient = RestClient.create();
    @Value("${app.storage.cloudinary.cloud-name}") private String cloudName;
    @Value("${app.storage.cloudinary.api-key}") private String apiKey;
    @Value("${app.storage.cloudinary.api-secret}") private String apiSecret;

    @Override public String store(MultipartFile file, String folder) {
        try { return upload(file.getBytes(), file.getOriginalFilename(), folder); }
        catch (IOException e) { throw new IllegalStateException("Could not read upload", e); }
    }
    @Override public String store(byte[] content, String filename, String contentType, String folder) { return upload(content, filename, folder); }

    @SuppressWarnings("unchecked")
    private String upload(byte[] bytes, String filename, String folder) {
        long timestamp = Instant.now().getEpochSecond();
        String publicId = (filename == null ? "document" : filename).replaceFirst("\\.[^.]+$", "").replaceAll("[^A-Za-z0-9_-]", "-");
        String signature = sha1("folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new ByteArrayResource(bytes) { @Override public String getFilename() { return filename == null ? "document" : filename; } });
        body.add("api_key", apiKey); body.add("timestamp", Long.toString(timestamp)); body.add("folder", folder);
        body.add("public_id", publicId); body.add("signature", signature);
        Map<String, Object> response = restClient.post()
                .uri("https://api.cloudinary.com/v1_1/" + cloudName + "/raw/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Map.class);
        if (response == null || response.get("secure_url") == null) throw new IllegalStateException("Cloudinary returned no download URL");
        return response.get("secure_url").toString();
    }
    private String sha1(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Could not sign Cloudinary upload", e); }
    }
    @Override public String getAccessUrl(String key) { return key; }
    @Override public Resource load(String key) { try { return new UrlResource(URI.create(key)); } catch (Exception e) { throw new IllegalStateException("Invalid Cloudinary URL", e); } }
    @Override public String getContentType(String key) { return key.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream"; }
}
