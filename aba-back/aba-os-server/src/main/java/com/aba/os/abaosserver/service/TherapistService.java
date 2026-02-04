package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.Therapist;
import com.aba.os.abaosserver.domain.User;
import com.aba.os.abaosserver.dto.therapist.TherapistCreateRequest;
import com.aba.os.abaosserver.dto.therapist.TherapistListResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistUpdateRequest;
import com.aba.os.abaosserver.repository.CenterRepository;
import com.aba.os.abaosserver.repository.TherapistRepository;
import com.aba.os.abaosserver.repository.UserRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TherapistService {

    private final TherapistRepository therapistRepository;
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final SecurityUtil securityUtil;

    /**
     * 치료사 목록 조회 (삭제되지 않은 치료사만)
     */
    public List<TherapistListResponse> getTherapists() {
        UUID centerId = securityUtil.getCurrentCenterId();

        List<Therapist> therapists = therapistRepository.findByCenter_IdAndDeletedFalse(centerId);

        return therapists.stream()
                .map(TherapistListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 치료사 상세 조회
     */
    public TherapistResponse getTherapistDetail(UUID therapistId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Therapist therapist = therapistRepository.findByIdAndDeletedFalse(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("치료사를 찾을 수 없습니다."));

        // 같은 센터 소속인지 검증
        if (!therapist.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return TherapistResponse.from(therapist);
    }

    /**
     * 치료사 등록
     */
    @Transactional
    public UUID createTherapist(TherapistCreateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 센터 조회
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터를 찾을 수 없습니다."));

        // 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 사용자가 같은 센터 소속인지 검증
        if (!user.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("해당 사용자는 소속 센터가 다릅니다.");
        }

        // 사용자가 이미 치료사로 등록되어 있는지 확인
        if (therapistRepository.existsByUserIdAndDeletedFalse(request.getUserId())) {
            throw new IllegalArgumentException("해당 사용자는 이미 치료사로 등록되어 있습니다.");
        }

        Therapist therapist = Therapist.builder()
                .user(user)
                .center(center)
                .specialty(request.getSpecialty())
                .experienceYears(request.getExperienceYears())
                .build();

        Therapist savedTherapist = therapistRepository.save(therapist);
        log.info("치료사 등록 완료 - ID: {}, 이름: {}", savedTherapist.getId(), user.getName());

        return savedTherapist.getId();
    }

    /**
     * 치료사 정보 수정
     */
    @Transactional
    public void updateTherapist(UUID therapistId, TherapistUpdateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Therapist therapist = therapistRepository.findByIdAndDeletedFalse(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("치료사를 찾을 수 없습니다."));

        // 권한 검증: 같은 센터 소속인지
        if (!therapist.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        therapist.update(request.getSpecialty(), request.getExperienceYears());
        log.info("치료사 정보 수정 완료 - ID: {}, 이름: {}", therapistId, therapist.getUser().getName());
    }

    /**
     * 치료사 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteTherapist(UUID therapistId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Therapist therapist = therapistRepository.findByIdAndDeletedFalse(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("치료사를 찾을 수 없습니다."));

        // 권한 검증: 같은 센터 소속인지
        if (!therapist.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        therapist.softDelete();
        log.info("치료사 삭제 완료 (Soft Delete) - ID: {}, 이름: {}", therapistId, therapist.getUser().getName());
    }
}
