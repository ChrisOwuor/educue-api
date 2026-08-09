package com.owuor.educue.admissions.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.common.report.InstitutionPdfHeaderRenderer;
import com.owuor.educue.finance.service.PeriodFeeItemService;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.finance.service.InstitutionFinanceConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class AdmissionPackPdfService {
    private final InstitutionPdfHeaderRenderer headerRenderer;
    private final InstitutionFinanceConfigurationService financeConfigurationService;
    private final PeriodFeeItemService periodFeeItemService;

    public byte[] generate(Application application, Enrollment enrollment) {
        try {
            var out = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 48, 48, 42, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfFooterPageEvent());
            document.open();
            offer(document, application, enrollment);
            acceptance(document, application, enrollment);
            medical(document, application);
            fees(document, enrollment);
            checklist(document, application);
            document.close(); return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Could not generate admission letter", e); }
    }

    private void header(Document d, String title) throws Exception {
        headerRenderer.render(d, "ADM/LETTER");
        Paragraph heading = new Paragraph(title, new Font(Font.HELVETICA, 13, Font.BOLD));
        heading.setAlignment(Element.ALIGN_CENTER); heading.setSpacingBefore(14); heading.setSpacingAfter(16); d.add(heading);
    }
    private void offer(Document d, Application a, Enrollment e) throws Exception {
        header(d, "ADMISSION LETTER");
        d.add(new Paragraph("Date: " + LocalDate.now(), normal())); d.add(new Paragraph("Application: " + a.getApplicationNumber(), normal()));
        d.add(new Paragraph("Admission number: " + e.getStudent().getAdmissionNumber(), normal())); d.add(Chunk.NEWLINE);
        d.add(new Paragraph("Dear " + a.getFullName() + ",", normal()));
        d.add(p("We are pleased to offer you admission to " + e.getCourse().getName() + " for the " + e.getIntake().getName() + " intake. Your reporting date is " + e.getIntake().getStartDate() + ". This offer is subject to verification of your original documents, completion of the attached medical and acceptance forms, and compliance with the institution's fee requirements."));
        d.add(p("Please read this admission letter carefully, complete the required sections, and present the requested documents when reporting. We look forward to welcoming you."));
        d.add(Chunk.NEWLINE); d.add(new Paragraph("Registrar / Admissions Office", normal()));
    }
    private void acceptance(Document d, Application a, Enrollment e) throws Exception {
        d.newPage(); header(d, "ACCEPTANCE OF OFFER");
        d.add(p("I, " + a.getFullName() + ", National ID/Document No. ____________________, accept the offer of admission to " + e.getCourse().getName() + ". I agree to follow the institution's academic, financial, conduct, safety, and data-protection policies."));
        lines(d, "Student signature", "Date", "Parent/Guardian name", "Parent/Guardian signature", "Emergency contact");
    }
    private void medical(Document d, Application a) throws Exception {
        d.newPage(); header(d, "CONFIDENTIAL MEDICAL EXAMINATION FORM");
        d.add(new Paragraph("Student: " + a.getFullName() + "     Application: " + a.getApplicationNumber(), normal()));
        d.add(p("To be completed by a registered medical practitioner. Information is used to support student welfare and reasonable accommodation."));
        lines(d, "Medical history / chronic conditions", "Allergies", "Current medication", "Immunisation status", "Physical examination findings", "Laboratory findings (if required)", "Fitness for study / clinical placement", "Recommended accommodations", "Practitioner's name and registration number", "Facility, signature, stamp and date");
    }
    private void fees(Document document, Enrollment enrollment)
            throws Exception {

        document.newPage();
        header(document, "FEES AND PAYMENT GUIDANCE");

        String admissionNumber =
                enrollment.getStudent().getAdmissionNumber();

        document.add(p(
                "Use your admission number "
                + admissionNumber
                + " as the student reference for every payment. "
                + "Pay only through payment channels officially published "
                + "by the institution. Keep the transaction confirmation or "
                + "bank receipt and submit it to the finance office where required. "
                + "Do not send money to personal phone numbers."
        ));

        var paymentConfiguration = financeConfigurationService.get();

        if (paymentConfiguration.paybillShortCode() != null
            && !paymentConfiguration.paybillShortCode().isBlank()) {

            document.add(new Paragraph(
                    "M-PESA Paybill: "
                    + paymentConfiguration.paybillShortCode(),
                    new Font(Font.HELVETICA, 11, Font.BOLD)
            ));

            String accountInstructions =
                    paymentConfiguration.accountReferenceInstructions();

            document.add(p(
                    accountInstructions == null || accountInstructions.isBlank()
                            ? "Use the admission number as the account reference."
                            : accountInstructions
            ));
        }

        var courseAcademicPeriod =
                enrollment.getCurrentCourseAcademicPeriod();
        var schedule = periodFeeItemService.getEffectiveFees(
                courseAcademicPeriod.getUuid(), enrollment.getIntake().getId());

        document.add(Chunk.NEWLINE);

        document.add(new Paragraph(
                "Approved Fee for Current Academic Period",
                new Font(Font.HELVETICA, 11, Font.BOLD)
        ));

        PdfPTable feeTable = new PdfPTable(4);
        feeTable.setWidthPercentage(100);
        feeTable.setSpacingBefore(6f);
        feeTable.setSpacingAfter(8f);
        feeTable.setWidths(new float[]{0.5f, 3.5f, 1.2f, 1.5f});
        tableHeader(feeTable, "#"); tableHeader(feeTable, "Fee item");
        tableHeader(feeTable, "Type"); tableHeader(feeTable, "Amount (KES)");
        int row = 1;
        for (var item : schedule.items()) {
            tableCell(feeTable, String.valueOf(row++), Element.ALIGN_CENTER);
            tableCell(feeTable, item.feeItemName(), Element.ALIGN_LEFT);
            tableCell(feeTable, item.mandatory() ? "Mandatory" : "Optional", Element.ALIGN_CENTER);
            tableCell(feeTable, formatAmount(item.amount()), Element.ALIGN_RIGHT);
        }
        totalRow(feeTable, "Mandatory total", schedule.mandatoryTotal());
        totalRow(feeTable, "Optional total", schedule.optionalTotal());
        totalRow(feeTable, "Grand total", schedule.grandTotal());

        document.add(feeTable);

        document.add(Chunk.NEWLINE);

        document.add(p(
                "Before reporting, confirm the required initial payment and "
                + "the current official payment instructions with the finance office. "
                + "Fees may only be changed through an authorised institutional "
                + "fee schedule."
        ));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount);
    }
    private void checklist(Document d, Application a) throws Exception {
        d.newPage(); header(d, "ADMISSION AND REPORTING CHECKLIST");
        String[] items = {"Signed acceptance form", "Completed and stamped medical form", "Original identification document and copies", "Original academic certificates/result slips and copies", "Passport-size photographs", "Proof of required fee payment", "Any course-specific professional or regulatory documents"};
        for (String item : items) d.add(new Paragraph("[   ]  " + item, normal()));
        d.add(Chunk.NEWLINE); d.add(new Paragraph("Applicant declaration", new Font(Font.HELVETICA, 11, Font.BOLD)));
        d.add(p("I confirm that the documents and information I provide are genuine and complete. I understand that false information may lead to withdrawal of admission."));
        lines(d, "Student signature", "Date", "Admissions officer", "Verification date");
    }
    private void lines(Document d, String... labels) throws DocumentException { for (String label : labels) { d.add(Chunk.NEWLINE); d.add(new Paragraph(label + ": __________________________________________________________", normal())); } }
    private void tableHeader(PdfPTable table, String value) { PdfPCell cell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE))); cell.setBackgroundColor(new Color(31, 55, 88)); cell.setPadding(7); table.addCell(cell); }
    private void tableCell(PdfPTable table, String value, int alignment) { PdfPCell cell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 9))); cell.setHorizontalAlignment(alignment); cell.setPadding(6); cell.setBorderColor(new Color(220, 224, 230)); table.addCell(cell); }
    private void totalRow(PdfPTable table, String label, BigDecimal value) { PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 9, Font.BOLD))); labelCell.setColspan(3); labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT); labelCell.setPadding(6); table.addCell(labelCell); PdfPCell amount = new PdfPCell(new Phrase(formatAmount(value), new Font(Font.HELVETICA, 9, Font.BOLD))); amount.setHorizontalAlignment(Element.ALIGN_RIGHT); amount.setPadding(6); table.addCell(amount); }
    private Paragraph p(String value) { Paragraph p = new Paragraph(value, normal()); p.setLeading(18); p.setSpacingAfter(10); return p; }
    private Font normal() { return new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK); }
}
