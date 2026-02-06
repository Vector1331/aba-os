package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalCategory;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.domain.Therapist;
import com.aba.os.abaosserver.dto.migration.ExtractedChildData;
import com.aba.os.abaosserver.dto.migration.ImageMigrationResponse;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final ChildRepository childRepository;
    private final GoalRepository goalRepository;
    private final CenterRepository centerRepository;
    private final TherapistRepository therapistRepository;
    private final SecurityUtil securityUtil;
    private final OpenAiService openAiService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024; // 20MB

    /**
     * 엑셀 파일 업로드 및 데이터 마이그레이션
     * Sheet 1 (Children): Name, BirthDate(yyyy-MM-dd), Gender(M/F), Diagnosis
     * Sheet 2 (Goals): ChildName, GoalContent, Category
     */
    @Transactional
    public MigrationResponse uploadExcel(MultipartFile file) {
        Long centerId = securityUtil.getCurrentCenterId();

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
    private Map<String, Integer> processGoalsSheet(Sheet sheet, Long centerId, List<String> errors) {
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

    // ==================== 이미지 기반 마이그레이션 ====================

    /**
     * 이미지에서 아동 데이터 추출 및 저장 (Vision AI 사용)
     * - No-Storage Strategy: 이미지를 디스크에 저장하지 않고 메모리에서 처리
     */
    @Transactional
    public ImageMigrationResponse migrateFromImage(MultipartFile file) {
        Long centerId = securityUtil.getCurrentCenterId();
        List<String> errors = new ArrayList<>();

        // 1. 파일 검증
        String validationError = validateImageFile(file);
        if (validationError != null) {
            return ImageMigrationResponse.error(validationError, null);
        }

        // 2. 센터 및 치료사 조회
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터를 찾을 수 없습니다."));

        Therapist defaultTherapist = therapistRepository.findFirstByCenter_Id(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "센터에 등록된 치료사가 없습니다. 먼저 치료사를 등록해주세요."));

        try {
            // 3. 이미지를 Base64로 인코딩 (메모리에서 처리)
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = determineMimeType(file);

            log.info("이미지 마이그레이션 시작 - 파일: {}, 크기: {}KB, MIME: {}",
                    file.getOriginalFilename(),
                    imageBytes.length / 1024,
                    mimeType);
            // 주의: base64Image는 로그에 남기지 않음 (보안 및 로그 폭탄 방지)

            // 4. Vision AI로 데이터 추출
            ExtractedChildData extractedData = openAiService.extractChildDataFromImage(base64Image, mimeType);

            // Base64 문자열 즉시 해제 (메모리 정리)
            base64Image = null;
            imageBytes = null;

            // 5. 추출 데이터 검증
            if (extractedData.getChildName() == null || extractedData.getChildName().isBlank()) {
                return ImageMigrationResponse.error(
                        "이미지에서 아동 이름을 추출할 수 없습니다. 이미지가 흐리거나 텍스트가 없을 수 있습니다.",
                        List.of("childName is null or empty"));
            }

            // 6. Child 엔티티 생성 및 저장
            Child child = createChildFromExtractedData(extractedData, center, defaultTherapist, errors);

            // 중복 체크
            if (child.getBirthDate() != null &&
                    childRepository.existsByCenter_IdAndNameAndBirthDate(
                            centerId, child.getName(), child.getBirthDate())) {
                log.warn("중복 아동 감지: {} ({})", child.getName(), child.getBirthDate());
                // 기존 아동 조회
                child = childRepository.findByCenter_IdAndName(centerId, child.getName())
                        .orElse(child);
            } else {
                child = childRepository.save(child);
                log.info("아동 저장 완료 - ID: {}, 이름: {}", child.getId(), child.getName());
            }

            // 7. Goal 엔티티 생성 및 저장
            int goalsCreated = 0;
            if (extractedData.getGoals() != null && !extractedData.getGoals().isEmpty()) {
                goalsCreated = createGoalsFromExtractedData(extractedData.getGoals(), child, errors);
            }

            // 8. 응답 반환
            if (errors.isEmpty()) {
                return ImageMigrationResponse.success(
                        child.getId(),
                        child.getName(),
                        goalsCreated,
                        extractedData
                );
            } else {
                return ImageMigrationResponse.partialSuccess(
                        child.getId(),
                        child.getName(),
                        goalsCreated,
                        errors,
                        extractedData
                );
            }

        } catch (IOException e) {
            log.error("이미지 파일 읽기 실패", e);
            return ImageMigrationResponse.error(
                    "이미지 파일을 읽을 수 없습니다: " + e.getMessage(),
                    List.of(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("이미지 마이그레이션 실패", e);
            return ImageMigrationResponse.error(
                    "이미지 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    /**
     * 이미지 파일 검증
     */
    private String validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "파일이 비어있습니다.";
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return "지원하지 않는 이미지 형식입니다. (지원 형식: JPEG, PNG, WebP, GIF)";
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return "파일 크기가 너무 큽니다. (최대: 20MB)";
        }

        return null; // 검증 통과
    }

    /**
     * MIME 타입 결정
     */
    private String determineMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return contentType;
        }

        // 파일 확장자로 폴백
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (lower.endsWith(".png")) {
                return "image/png";
            } else if (lower.endsWith(".webp")) {
                return "image/webp";
            } else if (lower.endsWith(".gif")) {
                return "image/gif";
            }
        }

        return "image/jpeg"; // 기본값
    }

    /**
     * 추출 데이터로 Child 엔티티 생성
     */
    private Child createChildFromExtractedData(ExtractedChildData data, Center center,
                                                Therapist therapist, List<String> errors) {
        LocalDate birthDate = null;
        if (data.getBirthDate() != null && !data.getBirthDate().isBlank()) {
            try {
                birthDate = LocalDate.parse(data.getBirthDate(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.add("생년월일 파싱 실패: " + data.getBirthDate());
                log.warn("생년월일 파싱 실패: {}", data.getBirthDate());
            }
        }

        Child.Gender gender = Child.Gender.MALE; // 기본값
        if (data.getGender() != null) {
            String genderStr = data.getGender().toUpperCase().trim();
            if (genderStr.equals("FEMALE") || genderStr.equals("F") ||
                    genderStr.equals("여") || genderStr.equals("여자")) {
                gender = Child.Gender.FEMALE;
            }
        }

        return Child.builder()
                .center(center)
                .therapist(therapist)
                .name(data.getChildName().trim())
                .birthDate(birthDate)
                .gender(gender)
                .diagnosis(data.getDiagnosis())
                .status("active")
                .build();
    }

    /**
     * 추출된 목표 데이터로 Goal 엔티티들 생성
     */
    private int createGoalsFromExtractedData(List<ExtractedChildData.ExtractedGoal> extractedGoals,
                                              Child child, List<String> errors) {
        int created = 0;

        for (ExtractedChildData.ExtractedGoal extractedGoal : extractedGoals) {
            try {
                if (extractedGoal.getTitle() == null || extractedGoal.getTitle().isBlank()) {
                    errors.add("목표 제목이 비어있어 건너뜀");
                    continue;
                }

                GoalCategory category = parseCategory(extractedGoal.getCategory());
                GoalStatus status = parseGoalStatus(extractedGoal.getStatus());

                Goal goal = Goal.builder()
                        .child(child)
                        .name(extractedGoal.getTitle().trim())
                        .category(category)
                        .status(status)
                        .description(extractedGoal.getDescription())
                        .build();

                goalRepository.save(goal);
                created++;
                log.debug("목표 생성: {} ({})", goal.getName(), category);

            } catch (Exception e) {
                errors.add("목표 생성 실패: " + extractedGoal.getTitle() + " - " + e.getMessage());
                log.warn("목표 생성 실패: {}", e.getMessage());
            }
        }

        log.info("총 {}개 목표 생성 완료", created);
        return created;
    }

    /**
     * 목표 상태 문자열 파싱
     */
    private GoalStatus parseGoalStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return GoalStatus.IN_PROGRESS; // 기본값
        }

        try {
            return GoalStatus.valueOf(statusStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // 한글 매핑
            return switch (statusStr.trim()) {
                case "진행중", "진행 중", "수행중" -> GoalStatus.IN_PROGRESS;
                case "대기", "미시작" -> GoalStatus.WAITING;
                case "완료", "달성" -> GoalStatus.COMPLETED;
                case "유지" -> GoalStatus.MAINTENANCE;
                default -> GoalStatus.IN_PROGRESS;
            };
        }
    }
}
