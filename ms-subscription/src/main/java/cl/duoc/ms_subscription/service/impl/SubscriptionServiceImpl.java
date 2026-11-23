package cl.duoc.ms_subscription.service.impl;

import cl.duoc.ms_subscription.dto.PlansResponseDto;
import cl.duoc.ms_subscription.dto.SubscriptionRequestDto;
import cl.duoc.ms_subscription.dto.SubscriptionResponseDto;
import cl.duoc.ms_subscription.dto.UserResponseDto;
import cl.duoc.ms_subscription.model.Subscription;
import cl.duoc.ms_subscription.repository.SubscriptionRepository;
import cl.duoc.ms_subscription.service.SubscriptionService;
import cl.duoc.ms_subscription.service.api.PlansClient;
import cl.duoc.ms_subscription.service.api.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository repository;
    private final UserClient userClient;
    private final PlansClient plansClient;

    private SubscriptionResponseDto toDto(Subscription entity) {
        SubscriptionResponseDto dto = new SubscriptionResponseDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setPlansId(entity.getPlansId());
        dto.setEndDate(entity.getEndDate());
        dto.setState(entity.isState());
        dto.setStartDate(entity.getStartDate());
        dto.setAutoRenew(entity.isAutoRenew());
        dto.setPaymentMethod(entity.getPaymentMethod());
        UserResponseDto user = userClient.findById(entity.getUserId());
        if (user != null) {
            dto.setUserName(user.getPaternalSurname() + " " + user.getMaternalSurname());
            dto.setUserEmail(user.getEmail());
        }
        PlansResponseDto plan = plansClient.findById(entity.getPlansId());
        if (plan != null) {
            dto.setPlanName(plan.getName());
            dto.setPrice(plan.getPrice());
        }
        return dto;
    }



    private Subscription toEntity(SubscriptionResponseDto dto) {
        Subscription entity = toEntity(dto.getId(), dto.getUserId(), dto.getPlansId(), dto.getEndDate(),
                dto.isState(), dto.getStartDate(), dto.isAutoRenew(), dto.getPaymentMethod());
        return entity;
    }


    private Subscription toEntity(SubscriptionRequestDto dto) {
        return toEntity(dto.getId(), dto.getUserId(), dto.getPlansId(), dto.getEndDate(),
                dto.isState(), dto.getStartDate(), dto.isAutoRenew(), dto.getPaymentMethod());
    }

    private Subscription toEntity(Long id, Long userId, Long plansId, java.time.LocalDateTime endDate,
                                  boolean state, java.time.LocalDateTime startDate,
                                  boolean autoRenew, String paymentMethod) {
        Subscription entity = new Subscription();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setPlansId(plansId);
        entity.setEndDate(endDate);
        entity.setState(state);
        entity.setStartDate(startDate);
        entity.setAutoRenew(autoRenew);
        entity.setPaymentMethod(paymentMethod);
        return entity;
    }

    @Override
    public List<SubscriptionResponseDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public SubscriptionResponseDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElse(null);
    }

    @Override
    public List<SubscriptionResponseDto> findByUserId(Long userId) throws Exception {
        try {
            UserResponseDto userFind = userClient.findById(userId);

            if (userFind == null) {
                return null;
            }
            return repository.findByUserId(userId).stream().map(this::toDto).toList();

        } catch (Exception e) {

            throw new Exception(e.getMessage());
        }
    }

    @Override
    public List<SubscriptionResponseDto> findByPlansId(Long plansId) throws Exception {
        try {
            PlansResponseDto plansFind = plansClient.findById(plansId);

            if (plansFind == null) {
                return null;
            }
            return repository.findByPlansId(plansId).stream().map(this::toDto).toList();

        } catch (Exception e) {

            throw new Exception(e.getMessage());
        }
    }

    @Override
    public SubscriptionResponseDto create(SubscriptionRequestDto subscription) {
        validateReferences(subscription.getUserId(), subscription.getPlansId());
        if (subscription.getStartDate() == null) subscription.setStartDate(java.time.LocalDateTime.now());
        return toDto(repository.save(toEntity(subscription)));
    }

    @Override
    public SubscriptionResponseDto createForUser(Long userId, SubscriptionRequestDto subscription) {
        validateUser(userId);
        PlansResponseDto plan = validatePlan(subscription.getPlansId());
        if (plan.getDurationDays() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El plan seleccionado no es válido.");
        }
        LocalDateTime startDate = LocalDateTime.now();
        subscription.setUserId(userId);
        subscription.setStartDate(startDate);
        subscription.setEndDate(startDate.plusDays(plan.getDurationDays()));
        subscription.setState(true);
        subscription.setAutoRenew(false);
        return toDto(repository.save(toEntity(subscription)));
    }

    @Override
    public SubscriptionResponseDto update(Long id, SubscriptionRequestDto subscription) {
        if (repository.existsById(id)) {
            validateReferences(subscription.getUserId(), subscription.getPlansId());
            Subscription entity = toEntity(subscription);
            if (entity.getStartDate() == null) entity.setStartDate(java.time.LocalDateTime.now());
            entity.setId(id);
            return toDto(repository.save(entity));
        }
        return null;
    }

    private void validateReferences(Long userId, Long planId) {
        validateUser(userId);
        validatePlan(planId);
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario de la suscripción es obligatorio.");
        }

        UserResponseDto user;
        try {
            user = userClient.findById(userId);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario asociado a la suscripción no existe.", exception);
        }
        if (user == null || !userId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario asociado a la suscripción no existe.");
        }
    }

    private PlansResponseDto validatePlan(Long planId) {
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plan de la suscripción es obligatorio.");
        }

        PlansResponseDto plan;
        try {
            plan = plansClient.findById(planId);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plan asociado a la suscripción no existe.", exception);
        }
        if (plan == null || !planId.equals(plan.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plan asociado a la suscripción no existe.");
        }
        return plan;
    }

    @Override
    public SubscriptionResponseDto renewForUser(Long userId, Long subscriptionId) {
        return repository.findById(subscriptionId)
                .filter(subscription -> userId.equals(subscription.getUserId()))
                .map(subscription -> {
                    PlansResponseDto plan = validatePlan(subscription.getPlansId());
                    if (plan.getDurationDays() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El plan asociado no es válido.");
                    }
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime renewalStart = subscription.getEndDate() != null
                            && subscription.getEndDate().isAfter(now)
                            ? subscription.getEndDate()
                            : now;
                    subscription.setEndDate(renewalStart.plusDays(plan.getDurationDays()));
                    subscription.setState(true);
                    return toDto(repository.save(subscription));
                })
                .orElse(null);
    }

    @Override
    public SubscriptionResponseDto cancelForUser(Long userId, Long subscriptionId) {
        return repository.findById(subscriptionId)
                .filter(subscription -> userId.equals(subscription.getUserId()))
                .map(subscription -> {
                    subscription.setState(false);
                    subscription.setAutoRenew(false);
                    return toDto(repository.save(subscription));
                })
                .orElse(null);
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
