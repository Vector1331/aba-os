package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.*;
import com.aba.os.abaosserver.dto.session.*;
import com.aba.os.abaosserver.repository.*;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionTrialRepository sessionTrialRepository;
    private final ChildRepository childRepository;
    private final TherapistRepository therapistRepository;
    private final GoalRepository goalRepository;
    private final SecurityUtil securityUtil;

    /**
     * 세션 및 시행 기록 생성
     * - 트랜잭션 내에서 Session -> SessionTrial 순서로 저장
     * - Child의 lastSessionDate 업데이트
     */
    @Transactional
    public UUID createSession(SessionCreateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 아동 조회 및 검증
        Child child = childRepository.findById(request.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 치료사 조회 및 검증
        Therapist therapist = therapistRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new IllegalArgumentException("치료사를 찾을 수 없습니다."));

        if (!therapist.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("해당 치료사는 소속 센터가 다릅니다.");
        }

        // 1. Session 저장
        Session session = Session.builder()
                .child(child)
                .therapist(therapist)
                .sessionDate(request.getSessionDate())
                .duration(request.getDuration())
                .notes(request.getNotes())
                .build();

        Session savedSession = sessionRepository.save(session);

        // 2. SessionTrial 저장
        for (TrialRequest trialRequest : request.getTrials()) {
            // 목표 조회 및 검증
            Goal goal = goalRepository.findById(trialRequest.getGoalId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "목표를 찾을 수 없습니다: " + trialRequest.getGoalId()));

            // 해당 목표가 해당 아동의 목표인지 검증
            if (!goal.getChild().getId().equals(child.getId())) {
                throw new IllegalArgumentException(
                        "해당 목표는 이 아동의 목표가 아닙니다: " + trialRequest.getGoalId());
            }

            // 성공 횟수가 시행 횟수를 초과하지 않는지 검증
            if (trialRequest.getSuccesses() > trialRequest.getTrials()) {
                throw new IllegalArgumentException("성공 횟수는 시행 횟수를 초과할 수 없습니다.");
            }

            SessionTrial trial = SessionTrial.builder()
                    .session(savedSession)
                    .goal(goal)
                    .taskContent(trialRequest.getTaskContent())
                    .trials(trialRequest.getTrials())
                    .successes(trialRequest.getSuccesses())
                    .promptType(trialRequest.getPromptType())
                    .memo(trialRequest.getMemo())
                    .build();

            sessionTrialRepository.save(trial);
            savedSession.addTrial(trial);
        }

        // 3. Child의 lastSessionDate 업데이트
        child.updateLastSessionDate(request.getSessionDate());

        return savedSession.getId();
    }

    /**
     * 세션 목록 조회 (아동 ID로 필터링)
     */
    public List<SessionResponse> getSessions(UUID childId, LocalDate startDate, LocalDate endDate) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 아동 조회 및 검증
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // Fetch Join으로 N+1 방지
        List<Session> sessions = sessionRepository.findByChildIdAndDateRangeWithDetails(childId, startDate, endDate);

        return sessions.stream()
                .map(SessionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 세션 상세 조회
     */
    public SessionDetailResponse getSessionDetail(UUID sessionId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Session session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        // 같은 센터 소속인지 검증
        if (!session.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return SessionDetailResponse.from(session);
    }
}
