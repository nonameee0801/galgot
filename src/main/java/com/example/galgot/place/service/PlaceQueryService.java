package com.example.galgot.place.service;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 장소 조회(검색 + 페이징). 조건은 모두 선택이며, 넣은 조건만 AND로 적용된다. */
@Service
@Transactional(readOnly = true)
public class PlaceQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private final PlaceRepository placeRepository;

    public PlaceQueryService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    public Page<Place> search(String region, String signgu, Env env, Integer contentTypeId,
                              String keyword, int page, int size) {
        Specification<Place> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(region)) {
                predicates.add(cb.equal(root.get("ldongRegnCd"), region.trim()));
            }
            if (hasText(signgu)) {
                predicates.add(cb.equal(root.get("ldongSignguCd"), signgu.trim()));
            }
            if (env != null) {
                predicates.add(cb.equal(root.get("env"), env));
            }
            if (contentTypeId != null) {
                predicates.add(cb.equal(root.get("contentTypeId"), contentTypeId));
            }
            if (hasText(keyword)) {
                predicates.add(cb.like(root.get("title"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return placeRepository.findAll(spec, pageable(page, size));
    }

    public Optional<Place> find(Long id) {
        return placeRepository.findById(id);
    }

    /** 잘못된 값이 와도 오류 대신 안전한 범위로 보정한다. */
    static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by("title").ascending().and(Sort.by("id").ascending()));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
