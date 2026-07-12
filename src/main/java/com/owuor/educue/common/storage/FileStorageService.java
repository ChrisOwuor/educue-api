package com.owuor.educue.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, String folder);

    String getAccessUrl(String key);

    Resource load(String key);

    String getContentType(String key);
}
