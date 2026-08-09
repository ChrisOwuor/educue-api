package com.owuor.educue.auth.dto;import jakarta.validation.constraints.*;public record ResetPasswordRequest(@NotBlank String token,@NotBlank @Size(min=8,max=100) String newPassword){}
