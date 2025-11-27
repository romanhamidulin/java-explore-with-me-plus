package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public Page<UserDto> getUsers(UserRequest request) {

        Pageable pageable = PageRequest.of(request.getFrom(), request.getSize());

        Page<User> userPage = (request.getIds() == null || request.getIds().isEmpty())
                ? userRepository.findAll(pageable)
                : userRepository.findByIdIn(request.getIds(), pageable);

        return userPage.map(UserMapper::toUserDto);
    }

    @Override
    public UserDto createUser(NewUserRequest newUserRequest) {
        userRepository.findByEmail(newUserRequest.getEmail()).ifPresent(user -> {
            throw new ValidationException("Пользователь с таким email уже существует");
        });
        User user = UserMapper.toNewUser(newUserRequest);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        long deletedCount = userRepository.deleteByIdAndReturnCount(userId);

        if (deletedCount == 0) {
            throw new NotFoundException("Пользователь с Id " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }
}
