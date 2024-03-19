package ru.bereshs.HHWorkSearch.controller;

import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.bereshs.HHWorkSearch.config.AppConfig;
import ru.bereshs.HHWorkSearch.domain.KeyEntity;
import ru.bereshs.HHWorkSearch.domain.MessageEntity;
import ru.bereshs.HHWorkSearch.domain.ResumeEntity;
import ru.bereshs.HHWorkSearch.domain.SkillEntity;
import ru.bereshs.HHWorkSearch.exception.HhWorkSearchException;
import ru.bereshs.HHWorkSearch.hhApiClient.dto.HhEmployerDto;
import ru.bereshs.HHWorkSearch.hhApiClient.dto.HhListDto;
import ru.bereshs.HHWorkSearch.hhApiClient.dto.HhResumeDto;
import ru.bereshs.HHWorkSearch.hhApiClient.dto.HhViewsResume;
import ru.bereshs.HHWorkSearch.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@AllArgsConstructor
@Slf4j
public class ResumeController {

    private final HhService service;
    private final AppConfig config;
    private final AuthorizationService authorizationService;
    private final RatingEmployerService ratingEmployerService;
    private final SkillsEntityService skillsEntityService;
    private final MessageEntityService messageEntityService;
    private final ResumeEntityService resumeEntityService;

    @PostMapping("/api/resume/skill_set")
    public String postSkillSet(@RequestBody List<SkillEntity> skills) {
        skillsEntityService.saveAll(skills);
        return "ok";
    }

    @PatchMapping("/api/resume/skill_set/{id}")
    public String patchSkillEntityById(@PathVariable long id, @RequestBody SkillEntity entityDto) throws HhWorkSearchException {
        skillsEntityService.patchSkillEntityById(id, entityDto);
        return "ok";
    }

    @GetMapping("/api/resume/skill_set")
    public HhListDto<SkillEntity> getSkillsSet() {
        return skillsEntityService.getAll();
    }

    @PostMapping("/api/resume/message")
    public String postMessage(@RequestBody MessageEntity messageDto) {
        messageEntityService.save(messageDto);
        return "ok";
    }

    @PatchMapping("/api/resume/message/{id}")
    public String patchMessageById(@RequestBody MessageEntity messageDto, @PathVariable long id) throws HhWorkSearchException {
        messageEntityService.patchMessageById(id, messageDto);
        return "ok";
    }

    @GetMapping("/api/resume/{resumeId}/message/{messageId}")
    public String getMessageById(@PathVariable long messageId, @PathVariable String resumeId) throws HhWorkSearchException, IOException, ExecutionException, InterruptedException {
        MessageEntity message = messageEntityService.getMessageById(messageId);
        HhResumeDto resumeDto = service.getResumeById(resumeId, getToken());
        List<SkillEntity> skills = new ArrayList<>();
        for (String skill : resumeDto.getSkillSet()) {
            var elements = skill.split(" ");
            for (String element : elements) {
                skills.add(new SkillEntity(element.toLowerCase()));
            }
        }
        skillsEntityService.updateList(skills);
        String vacancyName = "Демонстрационная вакансия";
        return message.getMessage(skills, vacancyName);
    }

    @GetMapping("/api/resume/loyalEmployer")
    public Map<HhEmployerDto, Double> getLoyalEmployer() throws IOException, ExecutionException, InterruptedException {
        var resumes = service.getActiveResumes(getToken());
        var loyalEmployer = service.getLoyalEmployer(getToken(), resumes.getItems().get(0).getId());
        var difference = ratingEmployerService.getDifferenceAndUpdate(loyalEmployer);
        log.info("difference=" + difference);
        return loyalEmployer;
    }

    @GetMapping("/api/resume/views")
    public HhListDto<HhViewsResume> getViewsResumeList() throws IOException, ExecutionException, InterruptedException {

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
    public HhListDto<HhResumeDto> getMineResumes() throws IOException, ExecutionException, InterruptedException {
        HhListDto<HhResumeDto> resumeList  = service.getActiveResumes(getToken());
        List<ResumeEntity> resumeEntities = resumeList.getItems().stream().map(ResumeEntity::new).toList();
        resumeEntityService.saveAll(resumeEntities);
        return resumeList;
    }

    @PostMapping("/api/resume/default")
    public String setDefaultResume(@RequestBody HhResumeDto resumeDto) {
        ResumeEntity resume = resumeEntityService.getByHhid(resumeDto.getId());
        resume.setDefault(true);
        resumeEntityService.save(resume);

        return "ok";
    }

    @GetMapping("/api/resume/{resumeId}")
    public HhResumeDto getResumeById(@PathVariable String resumeId) throws IOException, ExecutionException, InterruptedException {
        return service.getResumeById(resumeId, getToken());
    }

    private OAuth2AccessToken getToken() throws IOException, ExecutionException, InterruptedException {
        return authorizationService.getToken();
    }
}