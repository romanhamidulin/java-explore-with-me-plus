package ru.practicum.user.service;

import org.springframework.data.domain.Page;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

public interface UserService {
    Page<UserDto> getUsers(UserRequest request);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);
}
