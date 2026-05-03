package com.team31.financetracker.transaction.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Category")
public class CategoryNode {

    @Id
    @GeneratedValue
    private Long id;              // Neo4j-internal graph ID

    @Property("category")
    private String category;      // e.g. "FOOD", "RENT", "SALARY"

    @Property("categoryType")
    private String categoryType;  // "INCOME_CATEGORY" | "EXPENSE_CATEGORY"

    // ── Constructors ──────────────────────────────────────────────────────────

    public CategoryNode() {}

    public CategoryNode(String category, String categoryType) {
        this.category     = category;
        this.categoryType = categoryType;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }
}