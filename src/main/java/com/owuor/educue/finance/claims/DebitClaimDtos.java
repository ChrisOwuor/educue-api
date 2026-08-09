package com.owuor.educue.finance.claims;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.*;
public final class DebitClaimDtos {private DebitClaimDtos(){}
 public record Item(@NotNull Long studentId,@NotNull @DecimalMin("0.01") BigDecimal amount){}
 public record Create(@NotBlank @Size(max=160) String title,@NotBlank String category,@NotBlank @Size(max=255) String reason,boolean allActiveStudents,@Valid List<Item> items,@DecimalMin("0.01") BigDecimal amountForAll){}
 public record View(UUID uuid,String title,String category,String reason,String status,int totalRows,int processedRows,int successfulRows,int failedRows,BigDecimal totalAmount,String sourceUrl,String resultUrl,String createdBy,String confirmedBy,String lastError,LocalDateTime createdAt,LocalDateTime completedAt){}
}
