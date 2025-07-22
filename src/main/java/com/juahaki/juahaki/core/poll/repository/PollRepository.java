package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findByStatus(PollStatus status);

    List<Poll> findByCategory(PollCategory category);

    List<Poll> findByCreator(User creator);

    Page<Poll> findByStatusOrderByCreatedAtDesc(PollStatus status, Pageable pageable);

    @Query("SELECT p FROM Poll p WHERE p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate > :now")
    List<Poll> findActivePolls(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Poll p WHERE p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate > :now")
    Page<Poll> findActivePollsPageable(@Param("now") LocalDateTime now, Pageable pageable);

    // Category-based queries
    Page<Poll> findByCategoryOrderByCreatedAtDesc(PollCategory category, Pageable pageable);

    Page<Poll> findByCategoryAndStatusOrderByCreatedAtDesc(PollCategory category, PollStatus status, Pageable pageable);

    // Creator-based queries
    Page<Poll> findByCreatorOrderByCreatedAtDesc(User creator, Pageable pageable);

    Page<Poll> findByCreatorAndStatusOrderByCreatedAtDesc(User creator, PollStatus status, Pageable pageable);

    @Query("SELECT p FROM Poll p WHERE " +
            "LOWER(p.title) LIKE %:searchTerm% OR " +
            "LOWER(p.description) LIKE %:searchTerm%")
    Page<Poll> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);


    List<Poll> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Poll> findByStartDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT p FROM Poll p WHERE p.endDate < :now AND p.status != 'CLOSED'")
    List<Poll> findExpiredPolls(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(p) FROM Poll p WHERE p.category = :category")
    long countByCategory(@Param("category") PollCategory category);

    @Query("SELECT COUNT(p) FROM Poll p WHERE p.creator = :creator")
    long countByCreator(@Param("creator") User creator);

    @Query("SELECT COUNT(p) FROM Poll p WHERE p.createdAt >= :date")
    long countCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT p FROM Poll p WHERE p.status = 'ACTIVE' ORDER BY p.totalVotes DESC")
    Page<Poll> findPopularActivePolls(Pageable pageable);

    Page<Poll> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    List<Poll> findByAllowOpinionsTrue();

    List<Poll> findByAllowAnonymousVotingTrue();

}
