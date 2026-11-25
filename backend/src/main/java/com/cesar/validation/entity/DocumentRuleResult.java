package com.cesar.validation.entity;

import com.cesar.validation.enums.RuleOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "document_rule_results")
public class DocumentRuleResult {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentSubmission document;

    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 160)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private RuleOutcome outcome;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    public DocumentRuleResult() {
    }

    public DocumentRuleResult(
        UUID id,
        int ruleOrder,
        String ruleCode,
        String ruleName,
        RuleOutcome outcome,
        String message
    ) {
        this.id = id;
        this.ruleOrder = ruleOrder;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.outcome = outcome;
        this.passed = outcome == RuleOutcome.PASSED;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DocumentSubmission getDocument() {
        return document;
    }

    public void setDocument(DocumentSubmission document) {
        this.document = document;
    }

    public int getRuleOrder() {
        return ruleOrder;
    }

    public void setRuleOrder(int ruleOrder) {
        this.ruleOrder = ruleOrder;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public RuleOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(RuleOutcome outcome) {
        this.outcome = outcome;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
