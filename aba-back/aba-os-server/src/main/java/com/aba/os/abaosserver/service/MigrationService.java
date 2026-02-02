package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalCategory;
import com.aba.os.abaosserver.domain.Therapist;
import com.aba.os.abaosserver.dto.migration.MigrationResponse;
import com.aba.os.abaosserver.repository.CenterRepository;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.GoalRepository;
import com.aba.os.abaosserver.repository.TherapistRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final ChildRepository childRepository;
    private final GoalRepository goalRepository;
    private final CenterRepository centerRepository;
    private final TherapistRepository therapistRepository;
    private final SecurityUtil securityUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 엑셀 파일 업로드 및 데이터 마이그레이션
     * Sheet 1 (Children): Name, BirthDate(yyyy-MM-dd), Gender(M/F), Diagnosis
     * Sheet 2 (Goals): ChildName, GoalContent, Category
     */
    @Transactional
    public MigrationResponse uploadExcel(MultipartFile file) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 파일 검증
        if (file.isEmpty()) {
            return MigrationResponse.error("파일이 비어있습니다.", null);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return MigrationResponse.error("xlsx 형식의 파일만 업로드 가능합니다.", null);
        }

        // 센터 조회
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터를 찾을 수 없습니다."));

        // 기본 치료사 조회 (마이그레이션용 - 센터의 첫 번째 치료사 사용)
        Therapist defaultTherapist = therapistRepository.findFirstByCenter_Id(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터에 등록된 치료사가 없습니다. 먼저 치료사를 등록해주세요."));

        List<String> errors = new ArrayList<>();
        int childrenSuccess = 0;
        int childrenSkipped = 0;
        int goalsSuccess = 0;
        int goalsFailure = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Sheet 1: Children 처리
            Sheet childrenSheet = workbook.getSheetAt(0);
            if (childrenSheet != null) {
                Map<String, Integer> result = processChildrenSheet(childrenSheet, center, defaultTherapist, errors);
                childrenSuccess = result.getOrDefault("success", 0);
                childrenSkipped = result.getOrDefault("skipped", 0);
            }

            // Sheet 2: Goals 처리
            if (workbook.getNumberOfSheets() > 1) {
                Sheet goalsSheet = workbook.getSheetAt(1);
                if (goalsSheet != null) {
                    Map<String, Integer> result = processGoalsSheet(goalsSheet, centerId, errors);
                    goalsSuccess = result.getOrDefault("success", 0);
                    goalsFailure = result.getOrDefault("failure", 0);
                }
            }

        } catch (IOException e) {
            log.error("엑셀 파일 처리 중 오류 발생", e);
            return MigrationResponse.error("파일 처리 중 오류가 발생했습니다: " + e.getMessage(), errors);
        }

        MigrationResponse response = MigrationResponse.success(childrenSuccess, childrenSkipped, goalsSuccess, goalsFailure);
        if (!errors.isEmpty()) {
            return MigrationResponse.builder()
                    .childrenSuccessCount(childrenSuccess)
                    .childrenSkippedCount(childrenSkipped)
                    .goalsSuccessCount(goalsSuccess)
                    .goalsFailureCount(goalsFailure)
                    .message(response.getMessage())
                    .errors(errors)
                    .build();
        }
        return response;
    }

    /**
     * Children 시트 처리
     */
    private Map<String, Integer> processChildrenSheet(Sheet sheet, Center center,
                                                       Therapist defaultTherapist, List<String> errors) {
        int success = 0;
        int skipped = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 헤더 스킵
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                String name = getCellStringValue(row.getCell(0));
                String birthDateStr = getCellStringValue(row.getCell(1));
                String genderStr = getCellStringValue(row.getCell(2));
                String diagnosis = getCellStringValue(row.getCell(3));

                if (name == null || name.isBlank()) {
                    errors.add("행 " + (i + 1) + ": 이름이 비어있습니다.");
                    continue;
                }

                LocalDate birthDate;
                try {
                    birthDate = LocalDate.parse(birthDateStr, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    errors.add("행 " + (i + 1) + ": 생년월일 형식 오류 (" + birthDateStr + ")");
                    continue;
                }

                // 중복 체크
                if (childRepository.existsByCenter_IdAndNameAndBirthDate(center.getId(), name, birthDate)) {
                    skipped++;
                    log.info("중복 아동 스킵: {} ({})", name, birthDate);
                    continue;
                }

                Child.Gender gender = parseGender(genderStr);

                Child child = Child.builder()
                        .center(center)
                        .therapist(defaultTherapist)
                        .name(name)
                        .birthDate(birthDate)
                        .gender(gender)
                        .diagnosis(diagnosis)
                        .status("active")
                        .build();

                childRepository.save(child);
                success++;

            } catch (Exception e) {
                errors.add("행 " + (i + 1) + ": " + e.getMessage());
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    /**
     * Goals 시트 처리
     */
    private Map<String, Integer> processGoalsSheet(Sheet sheet, UUID centerId, List<String> errors) {
        int success = 0;
        int failure = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 헤더 스킵
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                String childName = getCellStringValue(row.getCell(0));
                String goalContent = getCellStringValue(row.getCell(1));
                String categoryStr = getCellStringValue(row.getCell(2));

                if (childName == null || childName.isBlank()) {
                    errors.add("Goals 행 " + (i + 1) + ": 아동 이름이 비어있습니다.");
                    failure++;
                    continue;
                }

                if (goalContent == null || goalContent.isBlank()) {
                    errors.add("Goals 행 " + (i + 1) + ": 목표 내용이 비어있습니다.");
                    failure++;
                    continue;
                }

                // 아동 조회
                Optional<Child> childOpt = childRepository.findByCenter_IdAndName(centerId, childName);
                if (childOpt.isEmpty()) {
                    errors.add("Goals 행 " + (i + 1) + ": 아동을 찾을 수 없습니다 (" + childName + ")");
                    failure++;
                    continue;
                }

                GoalCategory category = parseCategory(categoryStr);

                Goal goal = Goal.builder()
                        .child(childOpt.get())
                        .name(goalContent)
                        .category(category)
                        .build();

                goalRepository.save(goal);
                success++;

            } catch (Exception e) {
                errors.add("Goals 행 " + (i + 1) + ": " + e.getMessage());
                failure++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("success", success);
        result.put("failure", failure);
        return result;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield date.format(DATE_FORMATTER);
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Child.Gender parseGender(String genderStr) {
        if (genderStr == null) return Child.Gender.MALE; // 기본값

        return switch (genderStr.toUpperCase().trim()) {
            case "F", "FEMALE", "여", "여자" -> Child.Gender.FEMALE;
            default -> Child.Gender.MALE;
        };
    }

    private GoalCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) {
            return GoalCategory.BEHAVIOR; // 기본값
        }

        try {
            return GoalCategory.valueOf(categoryStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // 한글 매핑
            return switch (categoryStr.trim()) {
                case "의사소통" -> GoalCategory.COMMUNICATION;
                case "사회성" -> GoalCategory.SOCIAL;
                case "감각통합", "감각" -> GoalCategory.SENSORY;
                case "신변처리", "자조" -> GoalCategory.SELF_CARE;
                case "인지" -> GoalCategory.COGNITIVE;
                case "운동", "대근육", "소근육" -> GoalCategory.MOTOR;
                case "놀이" -> GoalCategory.PLAY;
                case "행동" -> GoalCategory.BEHAVIOR;
                default -> GoalCategory.BEHAVIOR;
            };
        }
    }
}
