package com.owuor.educue.common.storage;
import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.core.io.*;import org.springframework.stereotype.Service;import org.springframework.web.multipart.MultipartFile;import software.amazon.awssdk.core.sync.RequestBody;import software.amazon.awssdk.services.s3.S3Client;import software.amazon.awssdk.services.s3.model.*;import java.util.UUID;
@Service @ConditionalOnProperty(name="app.storage.provider",havingValue="s3")
public class S3FileStorageService implements FileStorageService {
 private final S3Client s3;private final String bucket;
 public S3FileStorageService(S3Client s3,@Value("${app.storage.s3.bucket}")String bucket){this.s3=s3;this.bucket=bucket;}
 public String store(MultipartFile file,String folder){try{return store(file.getBytes(),file.getOriginalFilename(),file.getContentType(),folder);}catch(Exception e){throw new IllegalStateException("Could not read upload",e);}}
 public String store(byte[] content,String filename,String contentType,String folder){String key=folder+"/"+UUID.randomUUID()+"-"+(filename==null?"document":filename.replaceAll("[^A-Za-z0-9._-]","-"));s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).serverSideEncryption(ServerSideEncryption.AES256).build(),RequestBody.fromBytes(content));return key;}
 public String getAccessUrl(String key){return key;}
 public Resource load(String key){try{return new ByteArrayResource(s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray());}catch(Exception e){throw new IllegalStateException("Could not load S3 object",e);}}
 public String getContentType(String key){try{return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentType();}catch(Exception e){return "application/octet-stream";}}
}
