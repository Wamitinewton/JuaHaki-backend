package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.filters.OpinionFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.PollStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for creating JPA Specifications from filter requests.
 * Handles conversion of filter DTOs to database query specifications for polls and opinions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollFilterMapper {

    /**
     * Create JPA Specification for Poll filtering based on PollFilterRequest.
     *
     * @param filterRequest the filter criteria
     * @param currentUser the current user (for user-specific filters)
     * @return JPA Specification for Poll entity
     */
    public Specification<Poll> createPollSpecification(PollFilterRequest filterRequest, User currentUser) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest == null) {
                return criteriaBuilder.conjunction();
            }

            // Filter by category
            if (filterRequest.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), filterRequest.getCategory()));
            }

            // Filter by status
            if (filterRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filterRequest.getStatus()));
            }

            // Filter by active state
            if (filterRequest.getIsActive() != null) {
                if (filterRequest.getIsActive()) {
                    Predicate statusActive = criteriaBuilder.equal(root.get("status"), PollStatus.ACTIVE);
                    Predicate afterStart = criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), LocalDateTime.now());
                    Predicate beforeEnd = criteriaBuilder.greaterThan(root.get("endDate"), LocalDateTime.now());
                    predicates.add(criteriaBuilder.and(statusActive, afterStart, beforeEnd));
                } else {
                    // Non-active polls: either not ACTIVE status or expired
                    Predicate statusNotActive = criteriaBuilder.notEqual(root.get("status"), PollStatus.ACTIVE);
                    Predicate expired = criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), LocalDateTime.now());
                    predicates.add(criteriaBuilder.or(statusNotActive, expired));
                }
            }

            // Filter by search term (title or description)
            if (StringUtils.hasText(filterRequest.getSearchTerm())) {
                String searchPattern = "%" + filterRequest.getSearchTerm().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch));
            }

            // Filter by creation date range
            if (filterRequest.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), filterRequest.getCreatedAfter()));
            }
            if (filterRequest.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), filterRequest.getCreatedBefore()));
            }

            // Filter by start date range
            if (filterRequest.getStartDateAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("startDate"), filterRequest.getStartDateAfter()));
            }
            if (filterRequest.getStartDateBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("startDate"), filterRequest.getStartDateBefore()));
            }

            // Filter by creator username
            if (StringUtils.hasText(filterRequest.getCreatorUsername())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("creator").get("username"), filterRequest.getCreatorUsername()));
            }

            // Filter by anonymous voting setting
            if (filterRequest.getAllowAnonymousVoting() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("allowAnonymousVoting"), filterRequest.getAllowAnonymousVoting()));
            }

            // Filter by opinions setting
            if (filterRequest.getAllowOpinions() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("allowOpinions"), filterRequest.getAllowOpinions()));
            }

            if (currentUser != null) {
                if (filterRequest.getUserHasVoted() != null) {
                    if (filterRequest.getUserHasVoted()) {
                        Subquery<Long> voteSubquery = query.subquery(Long.class);
                        var voteRoot = voteSubquery.from(PollVote.class);
                        voteSubquery.select(voteRoot.get("poll").get("id"))
                                .where(criteriaBuilder.and(
                                        criteriaBuilder.equal(voteRoot.get("poll").get("id"), root.get("id")),
                                        criteriaBuilder.equal(voteRoot.get("user"), currentUser)
                                ));
                        predicates.add(criteriaBuilder.exists(voteSubquery));
                    } else {
                        // Polls where user has NOT voted - use NOT EXISTS subquery
                        Subquery<Long> voteSubquery = query.subquery(Long.class);
                        var voteRoot = voteSubquery.from(PollVote.class);
                        voteSubquery.select(voteRoot.get("poll").get("id"))
                                .where(criteriaBuilder.and(
                                        criteriaBuilder.equal(voteRoot.get("poll").get("id"), root.get("id")),
                                        criteriaBuilder.equal(voteRoot.get("user"), currentUser)
                                ));
                        predicates.add(criteriaBuilder.not(criteriaBuilder.exists(voteSubquery)));
                    }
                }

                if (filterRequest.getUserVoteChoice() != null) {
                    Subquery<Long> voteChoiceSubquery = query.subquery(Long.class);
                    var voteRoot = voteChoiceSubquery.from(PollVote.class);
                    voteChoiceSubquery.select(voteRoot.get("poll").get("id"))
                            .where(criteriaBuilder.and(
                                    criteriaBuilder.equal(voteRoot.get("poll").get("id"), root.get("id")),
                                    criteriaBuilder.equal(voteRoot.get("user"), currentUser),
                                    criteriaBuilder.equal(voteRoot.get("voteChoice"), filterRequest.getUserVoteChoice())
                            ));
                    predicates.add(criteriaBuilder.exists(voteChoiceSubquery));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create JPA Specification for Opinion filtering based on OpinionFilterRequest.
     *
     * @param filterRequest the filter criteria
     * @param currentUser the current user (for user-specific filters)
     * @return JPA Specification for PollOpinion entity
     */
    public Specification<PollOpinion> createOpinionSpecification(OpinionFilterRequest filterRequest, User currentUser) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest == null) {
                return criteriaBuilder.conjunction();
            }

            // Filter by poll ID
            if (filterRequest.getPollId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("poll").get("id"), filterRequest.getPollId()));
            }

            // Filter by stance/vote choice
            if (filterRequest.getStance() != null) {
                predicates.add(criteriaBuilder.equal(root.get("stance"), filterRequest.getStance()));
            }

            // Filter by anonymous opinions
            if (filterRequest.getIsAnonymous() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isAnonymous"), filterRequest.getIsAnonymous()));
            }

            // Filter by registered users only
            if (filterRequest.getFromRegisteredUsers() != null) {
                if (filterRequest.getFromRegisteredUsers()) {
                    // From registered users: author is not null and not anonymous
                    predicates.add(criteriaBuilder.and(
                            criteriaBuilder.isNotNull(root.get("author")),
                            criteriaBuilder.equal(root.get("isAnonymous"), false)
                    ));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("author")),
                            criteriaBuilder.equal(root.get("isAnonymous"), true)
                    ));
                }
            }

            // Filter by search term in content
            if (StringUtils.hasText(filterRequest.getSearchTerm())) {
                String searchPattern = "%" + filterRequest.getSearchTerm().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("content")), searchPattern));
            }

            // Filter by creation date range
            if (filterRequest.getCreatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), filterRequest.getCreatedAfter()));
            }
            if (filterRequest.getCreatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), filterRequest.getCreatedBefore()));
            }

            // Filter by minimum likes
            if (filterRequest.getMinLikes() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("likesCount"), filterRequest.getMinLikes().longValue()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create JPA Specification for Poll statistics based on PollStatsRequest.
     *
     * @param statsRequest the statistics filter criteria
     * @return JPA Specification for Poll entity
     */
    public Specification<Poll> createStatsSpecification(PollStatsRequest statsRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statsRequest == null) {
                return criteriaBuilder.conjunction();
            }

            // Filter by category for stats
            if (statsRequest.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), statsRequest.getCategory()));
            }

            // Filter by date range for stats
            if (statsRequest.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), statsRequest.getFromDate()));
            }
            if (statsRequest.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), statsRequest.getToDate()));
            }

            // Filter by creator for stats
            if (StringUtils.hasText(statsRequest.getCreatorUsername())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("creator").get("username"), statsRequest.getCreatorUsername()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification for trending polls (high engagement).
     *
     * @param hoursBack number of hours to look back for activity
     * @return JPA Specification for trending polls
     */
    public Specification<Poll> createTrendingPollsSpecification(int hoursBack) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only active polls
            predicates.add(criteriaBuilder.equal(root.get("status"), PollStatus.ACTIVE));

            // Created or updated recently
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursBack);
            Predicate recentlyCreated = criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), cutoffTime);
            Predicate recentlyUpdated = criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), cutoffTime);
            predicates.add(criteriaBuilder.or(recentlyCreated, recentlyUpdated));

            // Has significant engagement (votes or opinions)
            Predicate hasVotes = criteriaBuilder.greaterThan(root.get("totalVotes"), 0L);
            Predicate hasOpinions = criteriaBuilder.greaterThan(root.get("totalOpinions"), 0L);
            predicates.add(criteriaBuilder.or(hasVotes, hasOpinions));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification for expiring polls.
     *
     * @param hoursUntilExpiry number of hours until expiry
     * @return JPA Specification for expiring polls
     */
    public Specification<Poll> createExpiringPollsSpecification(int hoursUntilExpiry) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only active polls
            predicates.add(criteriaBuilder.equal(root.get("status"), PollStatus.ACTIVE));

            // Not yet expired
            predicates.add(criteriaBuilder.greaterThan(root.get("endDate"), LocalDateTime.now()));

            // Will expire within specified hours
            LocalDateTime expiryThreshold = LocalDateTime.now().plusHours(hoursUntilExpiry);
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), expiryThreshold));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    /**
     * Create specification for user's created polls with optional status filter.
     *
     * @param user the user who created the polls
     * @param status optional status filter
     * @return JPA Specification for user's polls
     */
    public Specification<Poll> createUserPollsSpecification(User user, PollStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Must be created by the user
            predicates.add(criteriaBuilder.equal(root.get("creator"), user));

            // Optional status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification for polls the user has participated in.
     *
     * @param user the user
     * @return JPA Specification for user's participated polls
     */
    public Specification<Poll> createUserParticipatedPollsSpecification(User user) {
        return (root, query, criteriaBuilder) -> {

            Subquery<Long> voteSubquery = query.subquery(Long.class);
            var voteRoot = voteSubquery.from(PollVote.class);
            voteSubquery.select(voteRoot.get("poll").get("id"))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(voteRoot.get("poll").get("id"), root.get("id")),
                            criteriaBuilder.equal(voteRoot.get("user"), user)
                    ));

            Subquery<Long> opinionSubquery = query.subquery(Long.class);
            var opinionRoot = opinionSubquery.from(PollOpinion.class);
            opinionSubquery.select(opinionRoot.get("poll").get("id"))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(opinionRoot.get("poll").get("id"), root.get("id")),
                            criteriaBuilder.equal(opinionRoot.get("author"), user)
                    ));

            return criteriaBuilder.or(
                    criteriaBuilder.exists(voteSubquery),
                    criteriaBuilder.exists(opinionSubquery)
            );
        };
    }
}
