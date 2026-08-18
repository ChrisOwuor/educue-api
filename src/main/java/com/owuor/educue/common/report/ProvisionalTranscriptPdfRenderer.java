package com.owuor.educue.common.report;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.results.dto.MyAcademicResultsResponse;
import com.owuor.educue.results.dto.StudentResultResponse;
import com.owuor.educue.results.dto.TranscriptYearGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ProvisionalTranscriptPdfRenderer {

    private final InstitutionPdfHeaderRendererBase headerRenderer;
    private final TranscriptPdfSections sections;

    public byte[] render(
            LinkedHashMap<String, String> studentDetails,
            MyAcademicResultsResponse academicResults
    ) {
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            24,
                            24,
                            26,
                            48
                    );

            PdfWriter writer =
                    PdfWriter.getInstance(
                            document,
                            out
                    );

            writer.setPageEvent(
                    new PdfFooterPageEvent()
            );

            document.open();

            headerRenderer.render(document);

            sections.addTitle(document);

            sections.addStudentDetails(
                    document,
                    studentDetails
            );

            List<TranscriptYearGroup> years =
                    buildYearGroups(
                            academicResults
                    );

            if (years.isEmpty()) {
                sections.addNoResults(document);
            } else {
                for (TranscriptYearGroup year : years) {
                    sections.addYear(
                            document,
                            year
                    );
                }

                TranscriptYearGroup latestYear =
                        years.getLast();

                sections.addRecommendation(
                        document,
                        latestYear.recommendation()
                );
            }

            sections.addLegend(document);
            sections.addProvisionalNotice(document);
            sections.addSignatures(document);

            document.close();

            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not generate provisional academic transcript",
                    exception
            );
        }
    }

    private List<TranscriptYearGroup> buildYearGroups(
            MyAcademicResultsResponse academicResults
    ) {
        if (academicResults == null ||
            academicResults.periods() == null ||
            academicResults.periods().isEmpty()) {

            return List.of();
        }

        Map<Integer,
                List<MyAcademicResultsResponse.AcademicPeriodResult>>
                periodsByYear =
                academicResults
                        .periods()
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        this::requireYearNumber,
                                        TreeMap::new,
                                        Collectors.toList()
                                )
                        );

        return periodsByYear
                .entrySet()
                .stream()
                .map(entry ->
                        createYearGroup(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .toList();
    }

    private TranscriptYearGroup createYearGroup(
            Integer yearNumber,
            List<MyAcademicResultsResponse.AcademicPeriodResult>
                    periods
    ) {
        List<MyAcademicResultsResponse.AcademicPeriodResult>
                orderedPeriods =
                periods.stream()
                        .sorted(
                                Comparator.comparing(
                                        period ->
                                                period.position() ==
                                                null
                                                        ? Integer.MAX_VALUE
                                                        : period.position()
                                )
                        )
                        .toList();

        BigDecimal currentAverage =
                calculateYearCurrentAverage(
                        orderedPeriods
                );

        MyAcademicResultsResponse.AcademicPeriodResult
                latestPeriod =
                orderedPeriods.getLast();

        /*
         * Show cumulative only when period 2 results
         * are present.
         *
         * For example:
         * Y1S1 -> no cumulative displayed
         * Y1S2 -> cumulative displayed
         */
        BigDecimal cumulativeAverage =
                containsSecondPeriod(
                        orderedPeriods
                )
                        ? latestPeriod.cumulativeAverage()
                        : null;

        List<StudentResultResponse> units =
                orderedPeriods.stream()
                        .flatMap(this::orderedUnits)
                        .toList();

        return new TranscriptYearGroup(
                yearNumber,
                "YEAR " + yearNumber,

                currentAverage,
                cumulativeAverage,

                latestPeriod.recommendation(),

                units
        );
    }

    /*
     * The yearly current average is the average of
     * the available period averages in that year.
     *
     * Year 1:
     * (Y1S1 average + Y1S2 average) / 2
     */
    private BigDecimal calculateYearCurrentAverage(
            List<MyAcademicResultsResponse.AcademicPeriodResult>
                    periods
    ) {
        List<BigDecimal> averages =
                periods.stream()
                        .map(
                                MyAcademicResultsResponse
                                        .AcademicPeriodResult
                                        ::currentAverage
                        )
                        .filter(value ->
                                value != null
                        )
                        .toList();

        if (averages.isEmpty()) {
            return null;
        }

        BigDecimal total =
                averages.stream()
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return total.divide(
                BigDecimal.valueOf(
                        averages.size()
                ),
                2,
                RoundingMode.HALF_UP
        );
    }

    private boolean containsSecondPeriod(
            List<MyAcademicResultsResponse.AcademicPeriodResult>
                    periods
    ) {
        return periods.stream()
                .anyMatch(period ->
                        period.periodNumber() != null &&
                        period.periodNumber() >= 2
                );
    }

    private Stream<StudentResultResponse> orderedUnits(
            MyAcademicResultsResponse.AcademicPeriodResult period
    ) {
        if (period.units() == null) {
            return Stream.empty();
        }

        return period.units()
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        StudentResultResponse::unitCode,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                                .thenComparing(
                                        StudentResultResponse::resultId
                                )
                );
    }

    private Integer requireYearNumber(
            MyAcademicResultsResponse.AcademicPeriodResult period
    ) {
        if (period.yearNumber() == null) {
            throw new IllegalStateException(
                    "Academic period " +
                    period.academicPeriodCode() +
                    " does not have a year number."
            );
        }

        return period.yearNumber();
    }
}
