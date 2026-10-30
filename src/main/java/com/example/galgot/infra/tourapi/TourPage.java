package com.example.galgot.infra.tourapi;

import java.util.List;

public record TourPage(int totalCount, List<TourPlaceItem> items) {
}
