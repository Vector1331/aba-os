package com.aba.os.abaosserver.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다.")
    private String name;

    // 추후 필요 시 추가 (전화번호, 프로필 이미지 등)
}
