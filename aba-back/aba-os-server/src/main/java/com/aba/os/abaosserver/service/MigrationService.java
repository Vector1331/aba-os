package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalCategory;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.domain.Session;
import com.aba.os.abaosserver.domain.SessionTrial;
import com.aba.os.abaosserver.domain.Therapist;
import com.aba.os.abaosserver.dto.migration.DiaMigrationResponse;
import com.aba.os.abaosserver.dto.migration.ExtractedChildData;
import com.aba.os.abaosserver.dto.migration.ImageMigrationResponse;
import com.aba.os.abaosserver.dto.migration.MigrationResponse;
import com.aba.os.abaosserver.repository.CenterRepository;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.GoalRepository;
import com.aba.os.abaosserver.repository.SessionRepository;
import com.aba.os.abaosserver.repository.SessionTrialRepository;
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
    private final SessionRepository sessionRepository;
    private final SessionTrialRepository sessionTrialRepository;
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

    // ==================== DIA 템플릿 엑셀 마이그레이션 ====================

    // DIA 템플릿 컬럼 인덱스 (0-based)
    private static final int COL_V = 21;  // V열 (아동명/날짜 라벨 또는 값)
    private static final int COL_W = 22;  // W열 (아동명/날짜 값)
    private static final int COL_C = 2;   // C열 (과제 내용)
    private static final int COL_H = 7;   // H열 (시행 데이터 시작)
    private static final int COL_Q = 16;  // Q열 (시행 데이터 끝)
    private static final int DATA_START_ROW = 3; // 데이터 시작 행 (0-based, 4행 = index 3)

    /**
     * DIA 템플릿 엑셀 파일 업로드 및 데이터 마이그레이션
     *
     * 템플릿 구조:
     * - V3: 아동명 (또는 "아동명" 라벨일 경우 W3에서 읽기)
     * - V4: 세션 날짜 (또는 "날짜" 라벨일 경우 W4에서 읽기)
     * - 4행부터 데이터 시작
     * - C열: 과제 내용 (task_content)
     * - H~Q열: 시행 마커 (+: 성공, -: 실패, p: 촉구)
     */
    @Transactional
    public DiaMigrationResponse uploadDiaExcel(MultipartFile file) {
        Long centerId = securityUtil.getCurrentCenterId();
        List<String> errors = new ArrayList<>();

        // 파일 검증
        if (file.isEmpty()) {
            return DiaMigrationResponse.error("파일이 비어있습니다.", null);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return DiaMigrationResponse.error("xlsx 형식의 파일만 업로드 가능합니다.", null);
        }

        // 센터 및 치료사 조회
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터를 찾을 수 없습니다."));

        Therapist defaultTherapist = therapistRepository.findFirstByCenter_Id(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "센터에 등록된 치료사가 없습니다. 먼저 치료사를 등록해주세요."));

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return DiaMigrationResponse.error("엑셀 파일에 시트가 없습니다.", null);
            }

            // 1. 아동명 추출 (V3 또는 W3)
            String childName = extractDiaValue(sheet, 2, COL_V, COL_W, "아동명");
            if (childName == null || childName.isBlank()) {
                return DiaMigrationResponse.error("아동명을 찾을 수 없습니다. V3 또는 W3 셀을 확인하세요.", null);
            }

            // 2. 세션 날짜 추출 (V4 또는 W4)
            String sessionDateStr = extractDiaValue(sheet, 3, COL_V, COL_W, "날짜");
            LocalDate sessionDate = parseDiaDate(sessionDateStr, errors);
            if (sessionDate == null) {
                sessionDate = LocalDate.now(); // 기본값
                errors.add("세션 날짜를 파싱할 수 없어 오늘 날짜로 설정했습니다: " + sessionDateStr);
            }

            log.info("DIA 마이그레이션 시작 - 아동: {}, 날짜: {}", childName, sessionDate);

            // 3. 아동 조회 또는 생성
            boolean childCreated = false;
            Child child = childRepository.findByCenter_IdAndName(centerId, childName).orElse(null);
            if (child == null) {
                // DIA 템플릿에서는 생년월일/성별 정보가 없으므로 기본값 설정
                child = Child.builder()
                        .center(center)
                        .therapist(defaultTherapist)
                        .name(childName)
                        .birthDate(LocalDate.of(2020, 1, 1)) // 기본 생년월일
                        .gender(Child.Gender.MALE) // 기본 성별
                        .status("active")
                        .build();
                child = childRepository.save(child);
                childCreated = true;
                log.info("새 아동 생성 - ID: {}, 이름: {} (생년월일/성별은 기본값)", child.getId(), childName);
                errors.add("아동 '" + childName + "'의 생년월일과 성별이 기본값으로 설정되었습니다. 필요시 수정해주세요.");
            }

            // 4. 기본 목표 조회 또는 생성 (DIA 마이그레이션용)
            Goal defaultGoal = goalRepository.findFirstByChildId(child.getId()).orElse(null);
            if (defaultGoal == null) {
                defaultGoal = Goal.builder()
                        .child(child)
                        .name("DIA 마이그레이션 목표")
                        .category(GoalCategory.BEHAVIOR)
                        .status(GoalStatus.ACTIVE)
                        .build();
                defaultGoal = goalRepository.save(defaultGoal);
                log.info("기본 목표 생성 - ID: {}", defaultGoal.getId());
            }

            // 5. 세션 생성
            Session session = Session.builder()
                    .child(child)
                    .therapist(defaultTherapist)
                    .sessionDate(sessionDate)
                    .duration(50) // 기본 50분
                    .notes("DIA 엑셀에서 마이그레이션됨")
                    .build();
            session = sessionRepository.save(session);
            log.info("세션 생성 - ID: {}, 날짜: {}", session.getId(), sessionDate);

            // 6. 데이터 행 처리 (4행부터)
            int trialsCreated = 0;
            int rowsProcessed = 0;
            int rowsSkipped = 0;
            int totalTrialCount = 0;
            int totalSuccessCount = 0;
            int totalPromptCount = 0;

            for (int rowIdx = DATA_START_ROW; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    rowsSkipped++;
                    continue;
                }

                // C열: 과제 내용
                String taskContent = getCellStringValue(row.getCell(COL_C));
                if (taskContent == null || taskContent.isBlank()) {
                    rowsSkipped++;
                    continue;
                }

                // H~Q열: 시행 마커 파싱
                DiaTrialData trialData = parseDiaTrialMarkers(row, COL_H, COL_Q);

                if (trialData.trials == 0) {
                    // 시행 데이터가 없는 행은 스킵
                    rowsSkipped++;
                    continue;
                }

                // SessionTrial 생성
                SessionTrial trial = SessionTrial.builder()
                        .session(session)
                        .goal(defaultGoal)
                        .taskContent(taskContent)
                        .trials(trialData.trials)
                        .successes(trialData.successes)
                        .promptCount(trialData.prompts)
                        .build();

                sessionTrialRepository.save(trial);
                session.addTrial(trial);

                trialsCreated++;
                rowsProcessed++;
                totalTrialCount += trialData.trials;
                totalSuccessCount += trialData.successes;
                totalPromptCount += trialData.prompts;

                log.debug("시행 기록 생성 - 과제: {}, 시행: {}, 성공: {}, 촉구: {}",
                        taskContent, trialData.trials, trialData.successes, trialData.prompts);
            }

            log.info("DIA 마이그레이션 완료 - 시행 기록: {}개, 처리 행: {}, 스킵 행: {}",
                    trialsCreated, rowsProcessed, rowsSkipped);

            // 7. 응답 반환
            if (errors.isEmpty()) {
                return DiaMigrationResponse.success(
                        child.getId(), childName, childCreated,
                        session.getId(), sessionDate, session.getDuration(),
                        trialsCreated, totalTrialCount, totalSuccessCount, totalPromptCount,
                        rowsProcessed, rowsSkipped
                );
            } else {
                return DiaMigrationResponse.partialSuccess(
                        child.getId(), childName, childCreated,
                        session.getId(), sessionDate, session.getDuration(),
                        trialsCreated, totalTrialCount, totalSuccessCount, totalPromptCount,
                        rowsProcessed, rowsSkipped, errors
                );
            }

        } catch (IOException e) {
            log.error("DIA 엑셀 파일 처리 중 오류 발생", e);
            return DiaMigrationResponse.error("파일 처리 중 오류가 발생했습니다: " + e.getMessage(), errors);
        }
    }

    /**
     * DIA 템플릿에서 값 추출 (V열 또는 W열)
     * V열이 라벨(예: "아동명", "날짜")이면 W열에서 값을 읽음
     */
    private String extractDiaValue(Sheet sheet, int rowIdx, int colV, int colW, String labelName) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return null;

        String vValue = getCellStringValue(row.getCell(colV));
        if (vValue == null) return null;

        // V열이 라벨인 경우 W열에서 값 읽기
        if (vValue.contains(labelName) || vValue.equals(labelName)) {
            return getCellStringValue(row.getCell(colW));
        }

        // V열이 직접 값인 경우
        return vValue;
    }

    /**
     * DIA 날짜 파싱 (다양한 형식 지원)
     */
    private LocalDate parseDiaDate(String dateStr, List<String> errors) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        // 숫자만 있는 경우 (예: 45678 - Excel 시리얼 날짜)
        try {
            double serialDate = Double.parseDouble(dateStr.trim());
            // Excel 시리얼 날짜 변환 (1900-01-01 기준)
            return LocalDate.of(1899, 12, 30).plusDays((long) serialDate);
        } catch (NumberFormatException ignored) {
            // 숫자가 아니면 다음 파싱 시도
        }

        // 다양한 날짜 형식 시도
        String[] patterns = {
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy.MM.dd",
                "yy-MM-dd",
                "yy/MM/dd",
                "MM/dd/yyyy",
                "dd/MM/yyyy"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // 다음 패턴 시도
            }
        }

        return null;
    }

    /**
     * DIA 시행 마커 파싱 (H~Q열)
     * +: 성공, -: 실패, p 또는 P: 촉구
     */
    private DiaTrialData parseDiaTrialMarkers(Row row, int startCol, int endCol) {
        int trials = 0;
        int successes = 0;
        int prompts = 0;

        for (int col = startCol; col <= endCol; col++) {
            Cell cell = row.getCell(col);
            String value = getCellStringValue(cell);

            if (value == null || value.isBlank()) {
                continue;
            }

            value = value.trim();

            if ("+".equals(value)) {
                trials++;
                successes++;
            } else if ("-".equals(value)) {
                trials++;
            } else if ("p".equalsIgnoreCase(value)) {
                trials++;
                prompts++;
            }
        }

        return new DiaTrialData(trials, successes, prompts);
    }

    /**
     * DIA 시행 데이터 내부 클래스
     */
    private record DiaTrialData(int trials, int successes, int prompts) {}
}
