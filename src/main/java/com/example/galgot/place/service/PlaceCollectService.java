package com.example.galgot.place.service;

import com.example.galgot.global.config.CollectProperties;
import com.example.galgot.infra.tourapi.TourApiClient;
import com.example.galgot.infra.tourapi.TourApiException;
import com.example.galgot.infra.tourapi.TourPage;
import com.example.galgot.infra.tourapi.TourPlaceItem;
import com.example.galgot.place.domain.Place;
import com.example.galgot.place.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** TourAPI 장소를 지역/유형별로 받아 contentId 기준으로 upsert한다. */
@Service
public class PlaceCollectService {

    private static final Logger log = LoggerFactory.getLogger(PlaceCollectService.class);

    private final TourApiClient client;
    private final PlaceRepository placeRepository;
    private final CollectProperties props;

    public PlaceCollectService(TourApiClient client, PlaceRepository placeRepository, CollectProperties props) {
        this.client = client;
        this.placeRepository = placeRepository;
        this.props = props;
    }

    public record Result(int calls, int inserted, int updated, boolean stoppedEarly) {
    }

    public Result collect() {
        int calls = 0;
        int inserted = 0;
        int updated = 0;
        boolean stoppedEarly = false;

        outer:
        for (String region : props.regions()) {
            for (int contentType : props.contentTypes()) {
                int page = 1;
                while (true) {
                    if (calls >= props.maxCallsPerRun()) {
                        log.warn("실행당 최대 호출 수({})에 도달해 수집을 중단합니다.", props.maxCallsPerRun());
                        stoppedEarly = true;
                        break outer;
                    }

                    TourPage result;
                    try {
                        result = client.fetchAreaBasedList(region, contentType, page, props.pageSize());
                    } catch (TourApiException e) {
                        log.error("수집 중단 (region={}, type={}, page={}): {}", region, contentType, page, e.getMessage());
                        stoppedEarly = true;
                        break outer;
                    }
                    calls++;

                    int[] counts = upsert(result.items());
                    inserted += counts[0];
                    updated += counts[1];

                    log.info("수집 region={} type={} page={} (받은 건수 {}, 전체 {})",
                            region, contentType, page, result.items().size(), result.totalCount());

                    boolean last = result.items().isEmpty() || (long) page * props.pageSize() >= result.totalCount();
                    if (last) {
                        break;
                    }
                    page++;
                }
            }
        }

        log.info("수집 종료: 호출 {}회, 신규 {}건, 갱신 {}건, 중단 여부 {}", calls, inserted, updated, stoppedEarly);
        return new Result(calls, inserted, updated, stoppedEarly);
    }

    /** @return [신규 건수, 갱신 건수] */
    private int[] upsert(List<TourPlaceItem> items) {
        if (items.isEmpty()) {
            return new int[]{0, 0};
        }

        Map<String, Place> existing = new HashMap<>();
        placeRepository.findAllByContentIdIn(
                items.stream().map(TourPlaceItem::contentId).collect(Collectors.toList())
        ).forEach(p -> existing.put(p.getContentId(), p));

        List<Place> toSave = new ArrayList<>();
        int inserted = 0;
        int updated = 0;
        for (TourPlaceItem item : items) {
            Place place = existing.get(item.contentId());
            if (place == null) {
                toSave.add(Place.from(item));
                inserted++;
            } else {
                place.apply(item);
                toSave.add(place);
                updated++;
            }
        }
        placeRepository.saveAll(toSave);
        return new int[]{inserted, updated};
    }
}
