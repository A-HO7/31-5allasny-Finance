package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.neo4j.CategoryNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link CategoryNode}.
 *
 * Used by S3-F11 to find-or-create category nodes in the recommendation graph.
 */
@Repository
public interface CategoryNodeRepository extends Neo4jRepository<CategoryNode, Long> {

    /**
     * Find a CategoryNode by its category string (e.g. "FOOD").
     */
    Optional<CategoryNode> findByCategory(String category);

    /**
     * Merge a CategoryNode (create if absent, update categoryType if present).
     */
    @Query("""
            MERGE (c:Category {category: $category})
            ON CREATE SET c.categoryType = $categoryType
            ON MATCH  SET c.categoryType = $categoryType
            RETURN c
            """)
    CategoryNode mergeCategory(@Param("category") String category,
                               @Param("categoryType") String categoryType);
}
