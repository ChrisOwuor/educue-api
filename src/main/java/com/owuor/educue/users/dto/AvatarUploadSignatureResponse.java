package com.owuor.educue.users.dto;

public record AvatarUploadSignatureResponse(String uploadUrl, String apiKey, long timestamp,
                                            String folder, String publicId, String signature) { }
