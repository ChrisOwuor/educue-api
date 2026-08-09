package com.owuor.educue.graduation.enums;

public enum AwardClassification {

    FIRST_CLASS_HONOURS(
            "First Class Honours"
    ),

    SECOND_CLASS_HONOURS_UPPER_DIVISION(
            "Second Class Honours Upper Division"
    ),

    SECOND_CLASS_HONOURS_LOWER_DIVISION(
            "Second Class Honours Lower Division"
    ),

    DISTINCTION(
            "Distinction"
    ),

    CREDIT(
            "Credit"
    ),

    MERIT(
            "Merit"
    ),

    PASS(
            "Pass"
    );

    private final String displayName;

    AwardClassification(
            String displayName
    ) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
