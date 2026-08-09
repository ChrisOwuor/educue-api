package com.owuor.educue.students.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import com.owuor.educue.students.dto.ExamCardVerificationResponse;
import com.owuor.educue.students.entity.ExamCard;
import com.owuor.educue.students.repository.EnrollmentRepository;
import com.owuor.educue.students.repository.ExamCardRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.finance.repository.FeeLedgerRepository;
import com.owuor.educue.academics.enums.AttemptType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamCardService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final ExamCardRepository examCardRepository;
    private final InstitutionProfileRepository institutionRepository;
    private final FeeLedgerRepository feeLedgerRepository;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public byte[] generateForStudent(Long userId, AttemptType attemptType, UUID courseAcademicPeriodUuid) {
        var enrollment = enrollmentRepository.findByStudentUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment not found"));
        AttemptType selectedType = attemptType == null ? AttemptType.NORMAL : attemptType;
        UUID selectedPeriodUuid = courseAcademicPeriodUuid;
        if (selectedType == AttemptType.NORMAL && selectedPeriodUuid == null) {
            selectedPeriodUuid = enrollment.getCurrentCourseAcademicPeriod().getUuid();
        }
        final UUID periodFilter = selectedPeriodUuid;
        var registrations = registrationRepository
                .findByEnrollmentStudentUserIdAndStatusOrderByCourseUnitPlacementUnitCode(userId, RegistrationStatus.ACTIVE)
                .stream().filter(r -> r.getAttemptType() == selectedType)
                .filter(r -> periodFilter == null || r.getCourseUnitPlacement().getCourseAcademicPeriod().getUuid().equals(periodFilter))
                .toList();
        if (registrations.isEmpty()) {
            throw new IllegalStateException("No active " + selectedType.name().toLowerCase() + " exam units match the selected academic period");
        }
        var period = registrations.getFirst().getCourseUnitPlacement().getCourseAcademicPeriod();
        if (selectedType != AttemptType.NORMAL) {
            var balance = feeLedgerRepository.getOutstandingBalance(enrollment.getStudent().getId());
            if (balance.signum() > 0) throw new IllegalStateException("Please pay the outstanding balance of KES " + balance.toPlainString() + " before downloading this exam card");
        }

        ExamCard card = examCardRepository.findByStudentIdAndCourseAcademicPeriodId(
                enrollment.getStudent().getId(), period.getId()).orElseGet(() -> {
            ExamCard created = new ExamCard();
            created.setStudent(enrollment.getStudent());
            created.setCourseAcademicPeriod(period);
            return examCardRepository.save(created);
        });
        return render(card, registrations);
    }

    @Transactional(readOnly = true)
    public ExamCardVerificationResponse verify(UUID code) {
        ExamCard card = examCardRepository.findByVerificationCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Exam card verification code not found"));
        var registrations = registrationRepository
                .findByEnrollmentStudentIdAndCourseUnitPlacementCourseAcademicPeriodIdAndStatus(
                        card.getStudent().getId(), card.getCourseAcademicPeriod().getId(), RegistrationStatus.ACTIVE);
        boolean isCurrentPeriod = enrollmentRepository.findByStudentId(card.getStudent().getId())
                .map(enrollment -> enrollment.getCurrentCourseAcademicPeriod().getId()
                        .equals(card.getCourseAcademicPeriod().getId()))
                .orElse(false);
        return new ExamCardVerificationResponse(isCurrentPeriod && !registrations.isEmpty(), code,
                card.getStudent().getAdmissionNumber(), card.getStudent().getFullName(),
                card.getCourseAcademicPeriod().getCourse().getName(),
                card.getCourseAcademicPeriod().getAcademicPeriod().getName(), card.getIssuedAt(),
                registrations.stream().map(item -> new ExamCardVerificationResponse.ExamUnit(
                        item.getCourseUnitPlacement().getUnit().getCode(),
                        item.getCourseUnitPlacement().getUnit().getName())).toList());
    }

    private byte[] render(ExamCard card, java.util.List<com.owuor.educue.students.entity.StudentUnitRegistration> registrations) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 35, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfFooterPageEvent());
            document.open();
            addHeader(document);

            Paragraph title = new Paragraph("OFFICIAL EXAMINATION CARD", new Font(Font.HELVETICA, 14, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER); title.setSpacingBefore(12); title.setSpacingAfter(14); document.add(title);

            var period = card.getCourseAcademicPeriod();
            PdfPTable identity = new PdfPTable(2); identity.setWidthPercentage(100); identity.setWidths(new float[]{1.4f, 4.6f});
            info(identity, "Student", card.getStudent().getFullName());
            info(identity, "Admission number", card.getStudent().getAdmissionNumber());
            info(identity, "Course", period.getCourse().getName());
            info(identity, "Academic period", period.getAcademicPeriod().getName());
            identity.setSpacingAfter(14); document.add(identity);

            Paragraph authorization = new Paragraph(
                    "This student is authorized to take the following examinations.",
                    new Font(Font.HELVETICA, 10, Font.BOLD));
            authorization.setSpacingAfter(8); document.add(authorization);

            PdfPTable units = new PdfPTable(3); units.setWidthPercentage(100); units.setWidths(new float[]{0.7f, 1.5f, 5f}); units.setHeaderRows(1);
            header(units, "No."); header(units, "Unit Code"); header(units, "Examination Unit");
            int number = 1;
            for (var registration : registrations) {
                cell(units, String.valueOf(number++));
                cell(units, registration.getCourseUnitPlacement().getUnit().getCode());
                cell(units, registration.getCourseUnitPlacement().getUnit().getName());
            }
            units.setSpacingAfter(16); document.add(units);

            PdfPTable verification = new PdfPTable(2); verification.setWidthPercentage(100); verification.setWidths(new float[]{4.5f, 1.5f});
            PdfPCell note = new PdfPCell(); note.setBorder(Rectangle.BOX); note.setBorderColor(java.awt.Color.LIGHT_GRAY); note.setPadding(10);
            note.addElement(new Paragraph("VERIFICATION", new Font(Font.HELVETICA, 9, Font.BOLD)));
            note.addElement(new Paragraph("Scan the QR code to confirm this card and its current examination units.", new Font(Font.HELVETICA, 8)));
            note.addElement(new Paragraph("Code: " + card.getVerificationCode(), new Font(Font.COURIER, 7)));
            note.addElement(new Paragraph("Issued: " + card.getIssuedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), new Font(Font.HELVETICA, 8)));
            verification.addCell(note);
            PdfPCell qr = new PdfPCell(qrImage(frontendBaseUrl.replaceAll("/+$", "") + "/verify/exam-card/" + card.getVerificationCode()));
            qr.setPadding(6); qr.setHorizontalAlignment(Element.ALIGN_CENTER); qr.setVerticalAlignment(Element.ALIGN_MIDDLE); verification.addCell(qr);
            document.add(verification);

            Paragraph warning = new Paragraph("This card is valid only with the student's institutional identification.", new Font(Font.HELVETICA, 8, Font.ITALIC, java.awt.Color.DARK_GRAY));
            warning.setAlignment(Element.ALIGN_CENTER); warning.setSpacingBefore(12); document.add(warning);
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Could not generate exam card", exception);
        }
    }

    private void addHeader(Document document) throws Exception {
        var institution = institutionRepository.findById(1L).orElse(null);
        PdfPTable table = new PdfPTable(2); table.setWidthPercentage(100); table.setWidths(new float[]{1.2f, 5f});
        File logo = new File("./storage/logo.png");
        PdfPCell logoCell;
        if (logo.isFile()) {
            Image logoImage = Image.getInstance(logo.getAbsolutePath());
            logoImage.scaleToFit(68, 68);
            logoCell = new PdfPCell(logoImage);
        } else {
            logoCell = new PdfPCell();
        }
        logoCell.setBorder(Rectangle.NO_BORDER); logoCell.setPadding(4); table.addCell(logoCell);
        PdfPCell text = new PdfPCell(); text.setBorder(Rectangle.NO_BORDER);
        Paragraph name = new Paragraph(institution == null ? "EduCue Training Institution" : institution.getName(), new Font(Font.HELVETICA, 16, Font.BOLD));
        name.setAlignment(Element.ALIGN_CENTER); text.addElement(name);
        if (institution != null) {
            Paragraph address = new Paragraph(java.util.stream.Stream.of(institution.getAddress(), institution.getPhone(), institution.getOfficialEmail()).filter(v -> v != null && !v.isBlank()).collect(java.util.stream.Collectors.joining("  |  ")), new Font(Font.HELVETICA, 8));
            address.setAlignment(Element.ALIGN_CENTER); text.addElement(address);
        }
        table.addCell(text); table.setSpacingAfter(5); document.add(table);
    }

    private Image qrImage(String value) throws Exception {
        var matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 180, 180);
        BufferedImage image = new BufferedImage(180, 180, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 180; x++) for (int y = 0; y < 180; y++) image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes);
        Image qr = Image.getInstance(bytes.toByteArray()); qr.scaleToFit(92, 92); return qr;
    }

    private void info(PdfPTable table, String label, String value) { header(table, label); cell(table, value); }
    private void header(PdfPTable table, String value) { PdfPCell c = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 8, Font.BOLD))); c.setBackgroundColor(new java.awt.Color(225,225,225)); c.setPadding(7); table.addCell(c); }
    private void cell(PdfPTable table, String value) { PdfPCell c = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 8))); c.setPadding(7); c.setBorderColor(java.awt.Color.LIGHT_GRAY); table.addCell(c); }
}
