package com.example.galgot.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 순서 재배치 요청. 바꿀 항목만 보내면 된다. */
public record ReorderRequest(@NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull Long coursePlaceId,
            @NotNull @Min(1) Integer dayNo,
            @NotNull @Min(1) Integer visitOrder
    ) {
    }
}
