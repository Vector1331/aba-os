package com.aba.os.abaosserver.config;

import com.aba.os.abaosserver.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile({"local", "default"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final TherapistRepository therapistRepository;
    private final ChildRepository childRepository;

    // 고정 UUID (기억하기 쉬운 형식)
    private static final UUID CENTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID THERAPIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // 테스트 계정 정보
    private static final String TEST_EMAIL = "test@aba.com";
    private static final String TEST_PASSWORD = "1234";
    private static final String TEST_INVITE_CODE = "TEST1234";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("Starting Test Data Initialization...");
        log.info("========================================");

        boolean dataCreated = false;

        // 1. Center 생성
        if (!centerRepository.existsById(CENTER_ID)) {
            createTestCenter();
            dataCreated = true;
        } else {
            log.info("Test Center already exists. Skipping...");
        }

        // 2. User (Therapist) 생성
        if (!userRepository.existsById(USER_ID)) {
            createTestUser();
            dataCreated = true;
        } else {
            log.info("Test User already exists. Skipping...");
        }

        // 3. Therapist 생성
        if (!therapistRepository.existsById(THERAPIST_ID)) {
            createTestTherapist();
            dataCreated = true;
        } else {
            log.info("Test Therapist already exists. Skipping...");
        }

        // 4. Child 생성
        if (!childRepository.existsById(CHILD_ID)) {
            createTestChild();
            dataCreated = true;
        } else {
            log.info("Test Child already exists. Skipping...");
        }

        // 결과 출력
        printTestDataInfo(dataCreated);
    }

    private void createTestCenter() {
        entityManager.createNativeQuery("""
            INSERT INTO centers (id, name, invite_code, address, total_rooms, location_cluster, created_at)
            VALUES (:id, :name, :inviteCode, :address, :totalRooms, :locationCluster, :createdAt)
            """)
                .setParameter("id", CENTER_ID)
                .setParameter("name", "테스트 센터")
                .setParameter("inviteCode", TEST_INVITE_CODE)
                .setParameter("address", "서울시 강남구 테스트로 123")
                .setParameter("totalRooms", 5)
                .setParameter("locationCluster", "강남")
                .setParameter("createdAt", LocalDateTime.now())
                .executeUpdate();

        log.info("Created Test Center: 테스트 센터 (ID: {})", CENTER_ID);
    }

    private void createTestUser() {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        entityManager.createNativeQuery("""
            INSERT INTO users (id, center_id, email, password_hash, name, role, is_active_subscription, created_at)
            VALUES (:id, :centerId, :email, :passwordHash, :name, :role, :isActiveSubscription, :createdAt)
            """)
                .setParameter("id", USER_ID)
                .setParameter("centerId", CENTER_ID)
                .setParameter("email", TEST_EMAIL)
                .setParameter("passwordHash", encodedPassword)
                .setParameter("name", "테스트 치료사")
                .setParameter("role", "THERAPIST")
                .setParameter("isActiveSubscription", false)
                .setParameter("createdAt", LocalDateTime.now())
                .executeUpdate();

        log.info("Created Test User: {} (ID: {})", TEST_EMAIL, USER_ID);
    }

    private void createTestTherapist() {
        entityManager.createNativeQuery("""
            INSERT INTO therapists (id, user_id, center_id, specialty, experience_years)
            VALUES (:id, :userId, :centerId, :specialty, :experienceYears)
            """)
                .setParameter("id", THERAPIST_ID)
                .setParameter("userId", USER_ID)
                .setParameter("centerId", CENTER_ID)
                .setParameter("specialty", "ABA 치료")
                .setParameter("experienceYears", 3)
                .executeUpdate();

        log.info("Created Test Therapist (ID: {})", THERAPIST_ID);
    }

    private void createTestChild() {
        entityManager.createNativeQuery("""
            INSERT INTO children (id, center_id, therapist_id, name, gender, birth_date, diagnosis,
                                  current_dev_level, parent_characteristics, request_details, status, created_at)
            VALUES (:id, :centerId, :therapistId, :name, :gender, :birthDate, :diagnosis,
                    :currentDevLevel, :parentCharacteristics, :requestDetails, :status, :createdAt)
            """)
                .setParameter("id", CHILD_ID)
                .setParameter("centerId", CENTER_ID)
                .setParameter("therapistId", THERAPIST_ID)
                .setParameter("name", "김철수")
                .setParameter("gender", "MALE")
                .setParameter("birthDate", LocalDate.of(2020, 1, 1))
                .setParameter("diagnosis", "자폐 스펙트럼 장애 (ASD)")
                .setParameter("currentDevLevel", "언어 발달 지연, 사회적 상호작용 개선 필요")
                .setParameter("parentCharacteristics", "적극적인 가정 연계 치료 희망")
                .setParameter("requestDetails", "눈 맞춤 및 호명 반응 개선")
                .setParameter("status", "active")
                .setParameter("createdAt", LocalDateTime.now())
                .executeUpdate();

        log.info("Created Test Child: 김철수 (ID: {})", CHILD_ID);
    }

    private void printTestDataInfo(boolean dataCreated) {
        log.info("");
        log.info("========================================");
        log.info("[Test Data Initialized]");
        log.info("========================================");
        log.info("");
        log.info("  Therapist Login: {} / {}", TEST_EMAIL, TEST_PASSWORD);
        log.info("  Invite Code: {}", TEST_INVITE_CODE);
        log.info("");
        log.info("  Test IDs (Copy & Paste):");
        log.info("  - Center ID:    {}", CENTER_ID);
        log.info("  - User ID:      {}", USER_ID);
        log.info("  - Therapist ID: {}", THERAPIST_ID);
        log.info("  - Child ID:     {}", CHILD_ID);
        log.info("");
        log.info("  Access Token Curl:");
        log.info("  curl -X POST http://localhost:8080/api/v1/auth/login \\");
        log.info("    -H \"Content-Type: application/json\" \\");
        log.info("    -d '{{\"email\":\"{}\",\"password\":\"{}\"}}'", TEST_EMAIL, TEST_PASSWORD);
        log.info("");
        log.info("  Swagger UI: http://localhost:8080/swagger-ui/index.html");
        log.info("========================================");

        if (!dataCreated) {
            log.info("  (All test data already existed - no new data created)");
            log.info("========================================");
        }
    }
}
