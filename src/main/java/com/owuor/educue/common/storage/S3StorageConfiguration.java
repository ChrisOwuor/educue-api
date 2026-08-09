package com.owuor.educue.common.storage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.context.annotation.*;import software.amazon.awssdk.regions.Region;import software.amazon.awssdk.services.s3.S3Client;
@Configuration @ConditionalOnProperty(name="app.storage.provider",havingValue="s3") public class S3StorageConfiguration {@Bean S3Client s3Client(@org.springframework.beans.factory.annotation.Value("${app.storage.s3.region}")String region){return S3Client.builder().region(Region.of(region)).build();}}
