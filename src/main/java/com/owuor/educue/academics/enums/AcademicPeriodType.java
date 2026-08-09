package com.owuor.educue.academics.enums;

public enum AcademicPeriodType {
    SEMESTER("S", "Semester"),
    TERM("T", "Term"),
    TRIMESTER("TR", "Trimester"),
    MODULE("M", "Module"),
    QUARTER("Q", "Quarter"),
    BLOCK("B", "Block");

    private final String codeToken;
    private final String displayName;
    AcademicPeriodType(String codeToken, String displayName) { this.codeToken = codeToken; this.displayName = displayName; }
    public String codeToken() { return codeToken; }
    public String displayName() { return displayName; }
}
