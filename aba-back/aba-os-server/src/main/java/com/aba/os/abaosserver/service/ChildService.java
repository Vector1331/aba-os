package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.domain.Therapist;
import com.aba.os.abaosserver.dto.child.ChildCreateRequest;
import com.aba.os.abaosserver.dto.child.ChildDetailResponse;
import com.aba.os.abaosserver.dto.child.ChildListResponse;
import com.aba.os.abaosserver.repository.*;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final TherapistRepository therapistRepository;
    private final CenterRepository centerRepository;
    private final GoalRepository goalRepository;
    private final SessionRepository sessionRepository;
    private final SecurityUtil securityUtil;

    public List<ChildListResponse> getChildren(UUID therapistId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        List<Child> children = childRepository.findByCenterIdAndOptionalTherapistId(centerId, therapistId);

        return children.stream()
                .map(ChildListResponse::from)
                .collect(Collectors.toList());
    }

    public ChildDetailResponse getChildDetail(UUID childId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        // 같은 센터 소속인지 검증
        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 통계 계산
        long totalSessions = sessionRepository.countByChildId(childId);
        long activeGoals = goalRepository.countByChildIdAndStatus(childId, GoalStatus.ACTIVE);

        // TODO: promptRatio 계산 로직 (세션 시행 데이터 기반)
        Double promptRatio = null;

        return ChildDetailResponse.from(child, totalSessions, activeGoals, promptRatio);
    }

    @Transactional
    public UUID createChild(ChildCreateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 센터 조회
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("센터를 찾을 수 없습니다."));

        // 치료사 조회 및 검증
        Therapist therapist = therapistRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new IllegalArgumentException("치료사를 찾을 수 없습니다."));

        // 치료사가 같은 센터 소속인지 검증
        if (!therapist.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("해당 치료사는 소속 센터가 다릅니다.");
        }

        Child child = Child.builder()
                .center(center)
                .therapist(therapist)
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .diagnosis(request.getDiagnosis())
                .currentDevLevel(request.getCurrentDevLevel())
                .parentCharacteristics(request.getParentCharacteristics())
                .requestDetails(request.getRequestDetails())
                .build();

        Child savedChild = childRepository.save(child);
        return savedChild.getId();
    }
}
