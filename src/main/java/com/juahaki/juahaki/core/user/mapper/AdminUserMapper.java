package com.juahaki.juahaki.core.user.mapper;

import com.juahaki.juahaki.core.user.dto.admin.AdminUserPageResponse;
import com.juahaki.juahaki.core.user.dto.admin.AdminUserResponse;
import com.juahaki.juahaki.core.user.model.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminUserMapper {

    private final ModelMapper modelMapper;

    public AdminUserPageResponse buildAdminUserPageResponse(Page<User> userPage) {
        List<AdminUserResponse> userResponses = userPage.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());

        return AdminUserPageResponse.builder()
                .users(userResponses)
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .pageSize(userPage.getSize())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    public AdminUserResponse mapToAdminUserResponse(User user) {
        return modelMapper.map(user, AdminUserResponse.class);
    }

    public List<AdminUserResponse> mapToAdminUserResponseList(List<User> users) {
        return users.stream()
                .map(user -> modelMapper.map(user, AdminUserResponse.class))
                .collect(Collectors.toList());
    }
}
