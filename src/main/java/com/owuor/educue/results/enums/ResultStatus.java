package com.owuor.educue.results.enums;

public enum ResultStatus {
    DRAFT,      // lecturer is still entering marks
    SUBMITTED,  // lecturer is done, awaiting approval
    APPROVED,   // exam board / HOD has signed off
    RELEASED,   // visible to the student
    WITHHELD,   // approved but deliberately not released (fee arrears, discipline, pending appeal)
}
