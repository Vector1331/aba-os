package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.child.ChildCreateRequest;
import com.aba.os.abaosserver.dto.child.ChildDetailResponse;
import com.aba.os.abaosserver.dto.child.ChildListResponse;
import com.aba.os.abaosserver.dto.child.ChildUpdateRequest;
import com.aba.os.abaosserver.service.ChildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
@Tag(name = "2. 아동 관리 (Child)")
public class ChildController {

    private final ChildService childService;

    @GetMapping
    @Operation(summary = "아동 목록 조회",
            description = "STEP 4-2: 담당 아동 목록을 조회합니다. therapistId로 필터링 가능")
    public ResponseEntity<ApiResponse<List<ChildListResponse>>> getChildren(
            @RequestParam(required = false) Long therapistId) {
        List<ChildListResponse> children = childService.getChildren(therapistId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{id}")
    @Operation(summary = "아동 상세 조회",
            description = "STEP 4-3: 아동의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> getChildDetail(@PathVariable Long id) {
        ChildDetailResponse child = childService.getChildDetail(id);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 등록",
            description = """
                    STEP 3-1: Admin이 치료 대상 아동을 등록합니다. (Admin 전용)

                    **필수:** therapistId (치료사 관리 → 사용자 ID로 치료사 조회에서 획득)
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "아동 등록 (남아)",
                                    summary = "STEP 3-1: 첫 번째 아동 등록",
                                    value = """
                                            {
                                              "name": "김철수",
                                              "birthDate": "2020-03-15",
                                              "gender": "MALE",
                                              "diagnosis": "자폐 스펙트럼 장애 (ASD)",
                                              "currentDevLevel": "언어 발달 지연, 눈 맞춤 어려움",
                                              "parentCharacteristics": "적극적인 가정 연계 치료 희망",
                                              "requestDetails": "눈 맞춤 및 호명 반응 개선",
                                              "therapistId": 1
                                            }
                                            """),
                            @ExampleObject(name = "아동 등록 (여아)",
                                    summary = "추가 아동 등록",
                                    value = """
                                            {
                                              "name": "김영희",
                                              "birthDate": "2019-08-20",
                                              "gender": "FEMALE",
                                              "diagnosis": "발달 지연",
                                              "currentDevLevel": "전반적인 발달 지연",
                                              "parentCharacteristics": "치료에 적극적",
                                              "requestDetails": "의사소통 능력 향상",
                                              "therapistId": 1
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<Long>> createChild(@Valid @RequestBody ChildCreateRequest request) {
        Long childId = childService.createChild(request);
        return ResponseEntity.ok(ApiResponse.success(childId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 정보 수정",
            description = "아동의 정보를 수정합니다. (Admin 전용)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "아동 정보 수정",
                            summary = "진단명/발달수준 업데이트",
                            value = """
                                    {
                                      "name": "김철수",
                                      "diagnosis": "자폐 스펙트럼 장애 (ASD) - 경증",
                                      "currentDevLevel": "눈 맞춤 개선 중, 호명 반응 향상"
                                    }
                                    """
                    ))
            )
    )
    public ResponseEntity<ApiResponse<String>> updateChild(
            @PathVariable Long id,
            @Valid @RequestBody ChildUpdateRequest request) {
        childService.updateChild(id, request);
        return ResponseEntity.ok(ApiResponse.success("아동 정보가 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 삭제",
            description = "아동을 삭제합니다. 연관된 목표, 세션 데이터도 함께 삭제됩니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> deleteChild(@PathVariable Long id) {
        childService.deleteChild(id);
        return ResponseEntity.ok(ApiResponse.success("아동이 삭제되었습니다."));
    }
}
