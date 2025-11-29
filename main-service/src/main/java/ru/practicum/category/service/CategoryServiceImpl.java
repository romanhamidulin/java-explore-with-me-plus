package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final EventRepository eventRepository;


    @Override
    public List<CategoryDto> findAll(Integer from, Integer size) {
        return repository.findAllByIdBetween(from, size).stream().map(CategoryMapper::mapToDto).toList();
    }

    @Override
    public CategoryDto findById(Long catId) {
        Category category = checkCategory(catId);
        return CategoryMapper.mapToDto(category);
    }

    @Override
    public CategoryDto createById(NewCategoryDto dto) {
        Category category = CategoryMapper.mapToPojo(dto);
        return CategoryMapper.mapToDto(repository.save(category));
    }

    @Override
    public CategoryDto updateById(Long catId, NewCategoryDto dto) {
        Category category = checkCategory(catId);
        category.setName(dto.getName());
        return CategoryMapper.mapToDto(repository.save(category));
    }

    @Override
    public void deleteById(Long catId) {
        if (eventRepository.existsByCategory_Id(catId)) {
            throw new ConflictException("Категория относиться к событию");
        }
        repository.deleteById(catId);
    }

    private Category checkCategory(Long catId) {
        return repository.findById(catId).orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }
}
