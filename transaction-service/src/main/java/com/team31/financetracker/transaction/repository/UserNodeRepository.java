package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link UserNode}.
 *
 * Used by S3-F11 (record spending pattern) and S3-F12 (category
 * recommendations) to read and write the recommendation graph.
 */
@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {

    /**
     * Find a UserNode by its PostgreSQL user ID (not the Neo4j internal id).
     */
    Optional<UserNode> findByUserId(Long userId);

    /**
     * Merge a UserNode (create if absent, update name/currency if present).
     * Returns the merged node with all its SPENT_ON relationships loaded.
     */
    @Query("""
            MERGE (u:User {userId: $userId})
            ON CREATE SET u.name = $name, u.currencyPreference = $currencyPreference
            ON MATCH  SET u.name = $name, u.currencyPreference = $currencyPreference
            RETURN u
            """)
    UserNode mergeUser(@Param("userId") Long userId,
                       @Param("name") String name,
                       @Param("currencyPreference") String currencyPreference);

    /**
     * S3-F12: Get category recommendations for a user.
     * Finds categories that similar users spent on, but the target user hasn't.
     * Returns list of {category, categoryType, score, averageAmount}.
     */
    @Query("""
            MATCH (target:User {userId: $userId})-[:SPENT_ON]->(userCategory:Category)
            WITH target, collect(userCategory.category) AS userCategories
            MATCH (other:User)-[:SPENT_ON]->(userCategory:Category)
            WHERE other <> target AND userCategory.category IN userCategories
            WITH target, userCategories, collect(DISTINCT other) AS similarUsers
            UNWIND similarUsers AS similarUser
            MATCH (similarUser)-[r:SPENT_ON]->(recCategory:Category)
            WHERE NOT recCategory.category IN userCategories
            WITH recCategory.category AS category, recCategory.categoryType AS categoryType,
                 count(DISTINCT similarUser) AS score,
                 avg(r.totalAmount) AS averageAmount
            ORDER BY score DESC, averageAmount DESC
            LIMIT $limit
            RETURN category, categoryType, score, averageAmount
            """)
    List<Map<String, Object>> getCategoryRecommendations(@Param("userId") Long userId,
                                                         @Param("limit") int limit);
}
