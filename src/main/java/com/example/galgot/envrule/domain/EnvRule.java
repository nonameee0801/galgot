package com.example.galgot.envrule.domain;

import com.example.galgot.place.domain.Env;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "env_rules")
public class EnvRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType;

    @Column(name = "match_value", nullable = false, length = 100)
    private String matchValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "env", nullable = false, length = 10)
    private Env env;

    @Column(name = "priority", nullable = false)
    private int priority;

    protected EnvRule() {
    }

    public Long getId() { return id; }
    public RuleType getRuleType() { return ruleType; }
    public String getMatchValue() { return matchValue; }
    public Env getEnv() { return env; }
    public int getPriority() { return priority; }
}
