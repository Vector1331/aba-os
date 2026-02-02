package com.aba.os.abaosserver.config;

import com.aba.os.abaosserver.domain.*;
import com.aba.os.abaosserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Component
@Profile({"local", "default"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

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
        Center center = Center.builder()
                .id(CENTER_ID)
                .name("테스트 센터")
                .inviteCode(TEST_INVITE_CODE)
                .address("서울시 강남구 테스트로 123")
                .totalRooms(5)
                .locationCluster("강남")
                .build();

        centerRepository.save(center);
        log.info("Created Test Center: {} (ID: {})", center.getName(), CENTER_ID);
    }

    private void createTestUser() {
        Center center = centerRepository.findById(CENTER_ID)
                .orElseThrow(() -> new IllegalStateException("Center must be created first"));

        User user = User.builder()
                .id(USER_ID)
                .center(center)
                .email(TEST_EMAIL)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .name("테스트 치료사")
                .role(User.UserRole.THERAPIST)
                .isActiveSubscription(false)
                .build();

        userRepository.save(user);
        log.info("Created Test User: {} (ID: {})", TEST_EMAIL, USER_ID);
    }

    private void createTestTherapist() {
        User user = userRepository.findById(USER_ID)
                .orElseThrow(() -> new IllegalStateException("User must be created first"));
        Center center = centerRepository.findById(CENTER_ID)
                .orElseThrow(() -> new IllegalStateException("Center must be created first"));

        Therapist therapist = Therapist.builder()
                .id(THERAPIST_ID)
                .user(user)
                .center(center)
                .specialty("ABA 치료")
                .experienceYears(3)
                .build();

        therapistRepository.save(therapist);
        log.info("Created Test Therapist (ID: {})", THERAPIST_ID);
    }

    private void createTestChild() {
        Center center = centerRepository.findById(CENTER_ID)
                .orElseThrow(() -> new IllegalStateException("Center must be created first"));
        Therapist therapist = therapistRepository.findById(THERAPIST_ID)
                .orElseThrow(() -> new IllegalStateException("Therapist must be created first"));

        Child child = Child.builder()
                .id(CHILD_ID)
                .center(center)
                .therapist(therapist)
                .name("김철수")
                .gender(Child.Gender.MALE)
                .birthDate(LocalDate.of(2020, 1, 1))
                .diagnosis("자폐 스펙트럼 장애 (ASD)")
                .currentDevLevel("언어 발달 지연, 사회적 상호작용 개선 필요")
                .parentCharacteristics("적극적인 가정 연계 치료 희망")
                .requestDetails("눈 맞춤 및 호명 반응 개선")
                .status("active")
                .build();

        childRepository.save(child);
        log.info("Created Test Child: {} (ID: {})", child.getName(), CHILD_ID);
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
