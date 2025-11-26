package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<UserDto> getUsers(UserRequest request) {

        Pageable pageable = PageRequest.of(request.getFrom(), request.getSize());
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return userRepository.findAll(pageable).stream().map(UserMapper::toUserDto).toList();
        }
        return userRepository.findByIdIn(request.getIds(), pageable).stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        User newUser = UserMapper.toUser(userDto);
        newUser = userRepository.save(newUser);
        return UserMapper.toUserDto(newUser);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь с Id " + userId + " не найден"));
        userRepository.deleteById(userId);
    }
}
