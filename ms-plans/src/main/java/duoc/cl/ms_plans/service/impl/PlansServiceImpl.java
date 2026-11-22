package duoc.cl.ms_plans.service.impl;

import duoc.cl.ms_plans.dto.PlansRequestDto;
import duoc.cl.ms_plans.dto.PlansResponseDto;
import duoc.cl.ms_plans.model.Plans;
import duoc.cl.ms_plans.repository.PlansRepository;
import duoc.cl.ms_plans.service.PlansService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlansServiceImpl implements PlansService {

    private final PlansRepository repository;

    private Plans toEntity(PlansResponseDto dto) {
        return toEntity(dto.getId(), dto.getName(), dto.getPrice(), dto.getDurationDays(), dto.getPeriod(),
                dto.getClassesPerMonth(), dto.getBenefits(), dto.isFeatured(), dto.isActive());
    }

    private Plans toEntity(PlansRequestDto dto) {
        return toEntity(dto.getId(), dto.getName(), dto.getPrice(), dto.getDurationDays(), dto.getPeriod(),
                dto.getClassesPerMonth(), dto.getBenefits(), Boolean.TRUE.equals(dto.getFeatured()),
                dto.getActive() == null || dto.getActive());
    }

    private Plans toEntity(Long id, String name, double price, int durationDays, String period,
                           String classesPerMonth, String benefits, boolean featured, boolean active) {
        Plans entity = new Plans();
        entity.setId(id);
        entity.setName(name);
        entity.setPrice(price);
        entity.setDurationDays(durationDays);
        entity.setPeriod(period);
        entity.setClassesPerMonth(classesPerMonth);
        entity.setBenefits(benefits);
        entity.setFeatured(featured);
        entity.setActive(active);
        return entity;
    }

    private PlansResponseDto toDto(Plans entity) {
        PlansResponseDto dto = new PlansResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPrice(entity.getPrice());
        dto.setDurationDays(entity.getDurationDays());
        dto.setPeriod(entity.getPeriod());
        dto.setClassesPerMonth(entity.getClassesPerMonth());
        dto.setBenefits(entity.getBenefits());
        dto.setFeatured(entity.isFeatured());
        dto.setActive(entity.isActive());
        return dto;
    }


    @Override
    public List<PlansResponseDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public PlansResponseDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElse(null);
    }

    @Override
    public PlansResponseDto create(PlansRequestDto plans) {
        return toDto(repository.save(toEntity(plans)));
    }

    @Override
    public PlansResponseDto update(Long id, PlansRequestDto plans) {
        if (repository.existsById(id)) {
            Plans entity = toEntity(plans);
            entity.setId(id);
            return toDto(repository.save(entity));
        }
        return null;
    }

    @Override
    public boolean deleteById(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
