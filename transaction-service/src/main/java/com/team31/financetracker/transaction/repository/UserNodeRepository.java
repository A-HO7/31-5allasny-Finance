package com.team31.financetracker.transaction.repository;

import com.team31.financetracker.transaction.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
