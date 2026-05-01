package com.team31.financetracker.transaction.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;


@Node("User")
public class UserNode {

    @Id
    @GeneratedValue
    private Long id;              // Neo4j-internal graph ID

    @Property("userId")
    private Long userId;          // Mirrors PG User.id

    @Property("name")
    private String name;

    @Property("currencyPreference")
    private String currencyPreference;

    /**
     * Outgoing SPENT_ON relationships to CategoryNodes.
     * Populated lazily by the repository; not required for node creation.
     */
    @Relationship(type = "SPENT_ON", direction = Relationship.Direction.OUTGOING)
    private List<SpentOnRelationship> spentOnRelationships = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public UserNode() {}

    public UserNode(Long userId, String name, String currencyPreference) {
        this.userId             = userId;
        this.name               = name;
        this.currencyPreference = currencyPreference;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrencyPreference() { return currencyPreference; }
    public void setCurrencyPreference(String currencyPreference) {
        this.currencyPreference = currencyPreference;
    }

    public List<SpentOnRelationship> getSpentOnRelationships() {
        return spentOnRelationships;
    }
    public void setSpentOnRelationships(List<SpentOnRelationship> spentOnRelationships) {
        this.spentOnRelationships = spentOnRelationships;
    }
}