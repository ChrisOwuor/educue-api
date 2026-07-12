package com.owuor.educue.results.dto;

import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;

import java.util.List;

public record BatchApprovalRequest(
        List<Long> resultIds,
        ResultStatus status // "APPROVE" or "REJECT"
) {}
