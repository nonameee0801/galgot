package com.example.galgot.envrule.domain;

public enum RuleType {
    PLACE,      // 특정 장소(contentId) 예외
    CATEGORY,   // 분류 코드(lclsSystm1/2/3 중 하나와 일치)
    KEYWORD     // 제목에 포함된 단어
}
