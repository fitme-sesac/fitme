package com.example.pproject.user.dto;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.Constant.SocialType;
import lombok.*;

import java.time.LocalDateTime;
/**
 * @deprecated UserDTO는 점진적으로 분리(UserRequestDTO / UserResponseDTO)하기 위한 레거시 DTO 입니다.
 *             신규 코드는 UserRequestDTO / UserResponseDTO를 사용하세요.
 */
@Deprecated
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Integer id;
    private String userid;
    private String password;
    private String email;
    private String birthday;
    private String username;
    private String postcode;
    private String address;
    private String detailAddress;
    private String extraAddress;
    private RoleType roleType;
    private SocialType socialType;
    private LocalDateTime modDate;
}
