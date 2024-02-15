package ru.bereshs.HHWorkSearch.controller;

import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.bereshs.HHWorkSearch.config.AppConfig;
import ru.bereshs.HHWorkSearch.domain.Employer;
import ru.bereshs.HHWorkSearch.domain.VacancyEntity;
import ru.bereshs.HHWorkSearch.domain.dto.SearchVacancyRequestDto;
import ru.bereshs.HHWorkSearch.exception.HhWorkSearchException;
import ru.bereshs.HHWorkSearch.hhApiClient.dto.*;
import ru.bereshs.HHWorkSearch.producer.KafkaProducer;
import ru.bereshs.HHWorkSearch.service.AuthorizationService;
import ru.bereshs.HHWorkSearch.domain.KeyEntity;
import ru.bereshs.HHWorkSearch.service.HhService;
import ru.bereshs.HHWorkSearch.service.VacancyEntityService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
public class ManagementController {
    private final HhService service;
    private final AppConfig config;
    private final AuthorizationService authorizationService;

    private final KafkaProducer producer;
    private final VacancyEntityService vacancyEntityService;
    @Value("${app.telegram.token}")
    private String token;

    @Value("${app.clientId}")
    private String clientId;

    public ManagementController(HhService service, AppConfig config, AuthorizationService authorizationService, KafkaProducer producer, VacancyEntityService vacancyEntityService) {
        this.service = service;
        this.config = config;
        this.authorizationService = authorizationService;
        this.producer = producer;
        this.vacancyEntityService = vacancyEntityService;
    }

    @GetMapping("/api/vacancy/recomended")
    public List<VacancyEntity> getRecommendedVacancyList() throws IOException, ExecutionException, InterruptedException, HhWorkSearchException {
        var result = getVacancyEntityList();
        return saveUniqueList(result);
    }

    @GetMapping("/api/negotiations")
    public HhListDto<HhNegotiationsDto> getNegotiationsList() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {

        return service.getHhNegotiationsDtoList(getToken());
    }

    @GetMapping("/api/resume/loyalEmployer")
    public Map<HhEmployerDto, Double> getLoyalEmployer() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {
        var resumes = service.getActiveResumes(getToken());
        return service.getLoyalEmployer(getToken(), resumes.getItems().get(0).getId());

    }

    @GetMapping("/api/resume/views")
    public HhListDto<HhViewsResume> getViewsResumeList() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {

        var resumes = service.getActiveResumes(getToken());
        HhListDto<HhViewsResume> list = new HhListDto<>();
        list.setItems(new ArrayList<>());
        for (HhResumeDto resume : resumes.getItems()) {
            list.getItems().addAll(
                    service.getHhViewsResumeDtoList(getToken(), resume.getId()).getItems()
            );
        }
        return list;
    }

    @GetMapping("/api/resume/mine")
    public HhListDto<HhResumeDto> getMineResumes() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {
        return service.getActiveResumes(getToken());
    }

    private OAuth2AccessToken getToken() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {
        KeyEntity key = authorizationService.getByClientId(config.getHhClientId());
        return authorizationService.getToken(key);
    }

    private List<VacancyEntity> getVacancyEntityList() throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {
        return service.getRecommendedVacancy(getToken()).stream().map(VacancyEntity::new).toList();
    }

    private List<VacancyEntity> saveUniqueList(List<VacancyEntity> list) {
        var unique = vacancyEntityService.getUnique(list);
        vacancyEntityService.saveAll(unique);
        return unique;
    }
}
