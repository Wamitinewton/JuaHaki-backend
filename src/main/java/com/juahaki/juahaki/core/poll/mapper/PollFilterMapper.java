package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.filters.OpinionFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.PollStatus;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for creating JPA Specifications from filter requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollFilterMapper {

    /**
     * Creates specification for Poll filtering.
     */
    public Specification<Poll> createPollSpecification(PollFilterRequest filterRequest, User currentUser) {
        return (root, query, cb) -> {
            if (filterRequest == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();
            log.debug("Building Poll specification with filters: {}", filterRequest);

            // Category and status
            addEqualPredicate(predicates, cb, root.get("category"), filterRequest.getCategory());
            addEqualPredicate(predicates, cb, root.get("status"), filterRequest.getStatus());

            // Active or inactive
            if (filterRequest.getIsActive() != null) {
                predicates.add(filterRequest.getIsActive()
                        ? buildActivePollPredicate(root, cb)
                        : buildInactivePollPredicate(root, cb));
            }

            // Search term (title or description)
            if (StringUtils.hasText(filterRequest.getSearchTerm())) {
                String pattern = "%" + filterRequest.getSearchTerm().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // Date ranges
            addDateRangePredicate(predicates, cb, root.get("createdAt"),
                    filterRequest.getCreatedAfter(), filterRequest.getCreatedBefore());
            addDateRangePredicate(predicates, cb, root.get("startDate"),
                    filterRequest.getStartDateAfter(), filterRequest.getStartDateBefore());

            // Creator, anonymous voting, opinions
            addEqualPredicate(predicates, cb, root.get("creator").get("username"), filterRequest.getCreatorUsername());
            addEqualPredicate(predicates, cb, root.get("allowAnonymousVoting"), filterRequest.getAllowAnonymousVoting());
            addEqualPredicate(predicates, cb, root.get("allowOpinions"), filterRequest.getAllowOpinions());

            if (currentUser != null) {
                if (Boolean.TRUE.equals(filterRequest.getUserHasVoted())) {
                    predicates.add(cb.exists(createVoteSubquery(root, query, cb, currentUser, null)));
                } else if (Boolean.FALSE.equals(filterRequest.getUserHasVoted())) {
                    predicates.add(cb.not(cb.exists(createVoteSubquery(root, query, cb, currentUser, null))));
                }

                if (filterRequest.getUserVoteChoice() != null) {
                    predicates.add(cb.exists(createVoteSubquery(root, query, cb, currentUser, filterRequest.getUserVoteChoice())));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates specification for filtering opinions.
     */
    public Specification<PollOpinion> createOpinionSpecification(OpinionFilterRequest filterRequest, User currentUser) {
        return (root, query, cb) -> {
            if (filterRequest == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();
            log.debug("Building Opinion specification with filters: {}", filterRequest);

            addEqualPredicate(predicates, cb, root.get("poll").get("id"), filterRequest.getPollId());
            addEqualPredicate(predicates, cb, root.get("stance"), filterRequest.getStance());
            addEqualPredicate(predicates, cb, root.get("isAnonymous"), filterRequest.getIsAnonymous());

            if (filterRequest.getFromRegisteredUsers() != null) {
                if (filterRequest.getFromRegisteredUsers()) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("author")),
                            cb.equal(root.get("isAnonymous"), false)
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.isNull(root.get("author")),
                            cb.equal(root.get("isAnonymous"), true)
                    ));
                }
            }

            if (StringUtils.hasText(filterRequest.getSearchTerm())) {
                String pattern = "%" + filterRequest.getSearchTerm().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("content")), pattern));
            }

            addDateRangePredicate(predicates, cb, root.get("createdAt"),
                    filterRequest.getCreatedAfter(), filterRequest.getCreatedBefore());
            if (filterRequest.getMinLikes() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("likesCount"), filterRequest.getMinLikes().longValue()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates specification for Poll statistics.
     */
    public Specification<Poll> createStatsSpecification(PollStatsRequest statsRequest) {
        return (root, query, cb) -> {
            if (statsRequest == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();
            log.debug("Building Poll Stats specification with filters: {}", statsRequest);

            addEqualPredicate(predicates, cb, root.get("category"), statsRequest.getCategory());
            addDateRangePredicate(predicates, cb, root.get("createdAt"),
                    statsRequest.getFromDate(), statsRequest.getToDate());
            addEqualPredicate(predicates, cb, root.get("creator").get("username"), statsRequest.getCreatorUsername());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Trending polls spec.
     */
    public Specification<Poll> createTrendingPollsSpecification(int hoursBack) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursBack);

            predicates.add(cb.equal(root.get("status"), PollStatus.ACTIVE));
            predicates.add(cb.or(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), cutoff),
                    cb.greaterThanOrEqualTo(root.get("updatedAt"), cutoff)
            ));
            predicates.add(cb.or(
                    cb.greaterThan(root.get("totalVotes"), 0L),
                    cb.greaterThan(root.get("totalOpinions"), 0L)
            ));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Expiring polls spec.
     */
    public Specification<Poll> createExpiringPollsSpecification(int hoursUntilExpiry) {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryThreshold = now.plusHours(hoursUntilExpiry);

            return cb.and(
                    cb.equal(root.get("status"), PollStatus.ACTIVE),
                    cb.greaterThan(root.get("endDate"), now),
                    cb.lessThanOrEqualTo(root.get("endDate"), expiryThreshold)
            );
        };
    }

    public Specification<Poll> createUserPollsSpecification(User user, PollStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("creator"), user));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Specification<Poll> createUserParticipatedPollsSpecification(User user) {
        return (root, query, cb) -> cb.or(
                cb.exists(createVoteSubquery(root, query, cb, user, null)),
                cb.exists(createOpinionSubquery(root, query, cb, user))
        );
    }


    private void addEqualPredicate(List<Predicate> predicates, CriteriaBuilder cb, Path<?> path, Object value) {
        if (value != null && !(value instanceof String str && !StringUtils.hasText(str))) {
            predicates.add(cb.equal(path, value));
        }
    }

    private void addDateRangePredicate(List<Predicate> predicates, CriteriaBuilder cb,
                                       Path<LocalDateTime> path, LocalDateTime after, LocalDateTime before) {
        if (after != null) predicates.add(cb.greaterThanOrEqualTo(path, after));
        if (before != null) predicates.add(cb.lessThanOrEqualTo(path, before));
    }

    private Predicate buildActivePollPredicate(Root<Poll> root, CriteriaBuilder cb) {
        LocalDateTime now = LocalDateTime.now();
        return cb.and(
                cb.equal(root.get("status"), PollStatus.ACTIVE),
                cb.lessThanOrEqualTo(root.get("startDate"), now),
                cb.greaterThan(root.get("endDate"), now)
        );
    }

    private Predicate buildInactivePollPredicate(Root<Poll> root, CriteriaBuilder cb) {
        LocalDateTime now = LocalDateTime.now();
        return cb.or(
                cb.notEqual(root.get("status"), PollStatus.ACTIVE),
                cb.lessThanOrEqualTo(root.get("endDate"), now)
        );
    }

    private Subquery<Long> createVoteSubquery(Root<Poll> root, CriteriaQuery<?> query,
                                              CriteriaBuilder cb, User user, Object voteChoice) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<PollVote> voteRoot = subquery.from(PollVote.class);
        List<Predicate> subPreds = new ArrayList<>();
        subPreds.add(cb.equal(voteRoot.get("poll").get("id"), root.get("id")));
        subPreds.add(cb.equal(voteRoot.get("user"), user));
        if (voteChoice != null) subPreds.add(cb.equal(voteRoot.get("voteChoice"), voteChoice));
        subquery.select(voteRoot.get("poll").get("id")).where(cb.and(subPreds.toArray(new Predicate[0])));
        return subquery;
    }

    private Subquery<Long> createOpinionSubquery(Root<Poll> root, CriteriaQuery<?> query,
                                                 CriteriaBuilder cb, User user) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<PollOpinion> opinionRoot = subquery.from(PollOpinion.class);
        subquery.select(opinionRoot.get("poll").get("id"))
                .where(cb.and(
                        cb.equal(opinionRoot.get("poll").get("id"), root.get("id")),
                        cb.equal(opinionRoot.get("author"), user)
                ));
        return subquery;
    }
}
