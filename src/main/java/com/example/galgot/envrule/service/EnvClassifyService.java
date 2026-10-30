package com.example.galgot.envrule.service;

import com.example.galgot.envrule.repository.EnvRuleRepository;
import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** DB의 규칙(env_rules)으로 모든 장소의 env를 다시 계산해 저장한다. */
@Service
public class EnvClassifyService {

    private static final Logger log = LoggerFactory.getLogger(EnvClassifyService.class);

    private final EnvRuleRepository ruleRepository;
    private final PlaceRepository placeRepository;
    private final EnvClassifier classifier;

    public EnvClassifyService(EnvRuleRepository ruleRepository,
                              PlaceRepository placeRepository,
                              EnvClassifier classifier) {
        this.ruleRepository = ruleRepository;
        this.placeRepository = placeRepository;
        this.classifier = classifier;
    }

    @Transactional
    public Map<Env, Integer> reclassifyAll() {
        List<EnvClassifier.Rule> rules = ruleRepository.findAll().stream()
                .map(r -> new EnvClassifier.Rule(r.getId(), r.getRuleType(), r.getMatchValue(), r.getEnv(), r.getPriority()))
                .toList();

        Map<Env, Integer> counts = new EnumMap<>(Env.class);
        for (Place place : placeRepository.findAll()) {
            EnvClassifier.Result result = classifier.classify(
                    new EnvClassifier.Target(place.getContentId(), place.getLclsSystm1(),
                            place.getLclsSystm2(), place.getLclsSystm3(), place.getTitle()),
                    rules);
            place.classify(result.env(), result.ruleId());
            counts.merge(result.env(), 1, Integer::sum);
        }

        log.info("분류 결과 (규칙 {}개 적용): INDOOR={}, OUTDOOR={}, BOTH={}",
                rules.size(),
                counts.getOrDefault(Env.INDOOR, 0),
                counts.getOrDefault(Env.OUTDOOR, 0),
                counts.getOrDefault(Env.BOTH, 0));
        return counts;
    }
}
