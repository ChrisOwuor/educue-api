package com.owuor.educue.common.storage;
import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.core.io.*;import org.springframework.stereotype.Service;import org.springframework.web.multipart.MultipartFile;import java.nio.file.*;
@Service @ConditionalOnProperty(name="app.storage.provider",havingValue="local")
public class LocalFileStorageService implements FileStorageService {
 private final Path root;
 public LocalFileStorageService(@Value("${app.storage.local.root:./storage/documents}")String root){this.root=Paths.get(root).toAbsolutePath().normalize();try{Files.createDirectories(this.root);}catch(Exception e){throw new IllegalStateException("Could not create local storage",e);}}
 public String store(MultipartFile file,String folder){try{return store(file.getBytes(),file.getOriginalFilename(),file.getContentType(),folder);}catch(Exception e){throw new IllegalStateException("Could not store upload",e);}}
 public String store(byte[] bytes,String filename,String contentType,String folder){try{String safeFolder=folder.replace('\\','/').replaceAll("(^|/)\\.\\.(/|$)","");String safeName=(filename==null?"document":filename).replaceAll("[^A-Za-z0-9._-]","-");Path target=root.resolve(safeFolder).resolve(safeName).normalize();if(!target.startsWith(root))throw new SecurityException("Invalid storage path");Files.createDirectories(target.getParent());Files.write(target,bytes,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);return root.relativize(target).toString().replace('\\','/');}catch(Exception e){throw new IllegalStateException("Could not store file",e);}}
 public String getAccessUrl(String key){return key;}
 public Resource load(String key){try{Path file=root.resolve(key).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file))throw new IllegalArgumentException("File not found");return new FileSystemResource(file);}catch(Exception e){throw new IllegalStateException("Could not load file",e);}}
 public String getContentType(String key){try{String type=Files.probeContentType(root.resolve(key));return type==null?"application/octet-stream":type;}catch(Exception e){return "application/octet-stream";}}
}
