package com.juahaki.juahaki.core.poll.service.query;

import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollDetailsResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollListResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollSummaryResponse;
import com.juahaki.juahaki.core.poll.mapper.PollFilterMapper;
import com.juahaki.juahaki.core.poll.mapper.PollQueryMapper;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.poll.repository.PollOpinionRepository;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.repository.PollVoteRepository;
import com.juahaki.juahaki.core.poll.service.opinion.IPollOpinionService;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.core.user.repository.UserRepository;
import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
import com.juahaki.juahaki.shared.exception.CustomException;
import com.juahaki.juahaki.shared.utils.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollQueryService implements IPollQueryService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollOpinionRepository pollOpinionRepository;
    private final UserRepository userRepository;
    private final PollQueryMapper pollQueryMapper;
    private final PollFilterMapper pollFilterMapper;
    private final IPollOpinionService pollOpinionService;
    private final JwtHelperService jwtHelperService;

    @Override
    @Transactional(readOnly = true)
    public PollDetailsResponse getPollDetails(Long pollId, HttpServletRequest request) {
        log.info("Getting poll details for ID: {}", pollId);

        Poll poll = getPollById(pollId);

        User currentUser = getOptionalUser(request);
        PollVote userVote = getUserVoteForPoll(poll, currentUser);
        boolean userHasOpinion = checkUserHasOpinion(poll, currentUser);

        List<OpinionResponse> topOpinions = pollOpinionService.getTopOpinions(pollId, 5, request);
        List<OpinionResponse> recentOpinions = pollOpinionService.getRecentOpinions(pollId, 5, request);
        int totalOpinionsCount = (int) pollOpinionRepository.countByPoll(poll);

        return pollQueryMapper.toPollDetailsResponse(poll, userVote, userHasOpinion,
                topOpinions, recentOpinions, totalOpinionsCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PollSummaryResponse getPollSummary(Long pollId, HttpServletRequest request) {
        log.debug("Getting poll summary for ID: {}", pollId);

        Poll poll = getPollById(pollId);
        User currentUser = getOptionalUser(request);
        PollVote userVote = getUserVoteForPoll(poll, currentUser);
        boolean userHasOpinion = checkUserHasOpinion(poll, currentUser);

        return pollQueryMapper.toPollSummaryResponse(poll, userVote, userHasOpinion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getFilteredPolls(PollFilterRequest filterRequest, Pageable pageable, HttpServletRequest request) {
        log.debug("Getting filtered polls with filter: {}", filterRequest);

        User currentUser = getOptionalUser(request);
        Specification<Poll> spec = pollFilterMapper.createPollSpecification(filterRequest, currentUser);

        Page<Poll> polls = pollRepository.findAll(spec, pageable);

        return polls.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getActivePolls(Pageable pageable, HttpServletRequest request) {
        log.debug("Getting active polls");

        LocalDateTime now = LocalDateTime.now();
        Page<Poll> activePolls = pollRepository.findActivePollsPageable(now, pageable);
        User currentUser = getOptionalUser(request);

        return activePolls.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getUserCreatedPolls(Pageable pageable, HttpServletRequest request) {
        log.debug("Getting user created polls");

        User currentUser = getAuthenticatedUser(request);
        Page<Poll> userPolls = pollRepository.findByCreatorOrderByCreatedAtDesc(currentUser, pageable);

        return userPolls.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getUserVotedPolls(Pageable pageable, HttpServletRequest request) {
        log.debug("Getting user voted polls");

        User currentUser = getAuthenticatedUser(request);

        List<PollVote> userVotes = pollVoteRepository.findByUserOrderByCreatedAtDesc(currentUser);
        List<Poll> votedPolls = userVotes.stream()
                .map(PollVote::getPoll)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), votedPolls.size());
        List<Poll> pagedPolls = votedPolls.subList(start, end);

        return pagedPolls.stream()
                .map(poll -> {
                    PollVote userVote = getUserVoteForPoll(poll, currentUser);
                    return pollQueryMapper.toPollListResponse(poll, userVote);
                })
                .toList()
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, votedPolls.size())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getPollsByCategory(PollCategory category, Pageable pageable, HttpServletRequest request) {
        log.debug("Getting polls by category: {}", category);

        Page<Poll> categoryPolls = pollRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        User currentUser = getOptionalUser(request);

        return categoryPolls.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> searchPolls(String searchTerm, Pageable pageable, HttpServletRequest request) {
        log.debug("Searching polls with term: {}", searchTerm);

        Page<Poll> searchResults = pollRepository.findBySearchTerm(searchTerm.toLowerCase(), pageable);
        User currentUser = getOptionalUser(request);

        return searchResults.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PollListResponse> getTrendingPolls(int limit, HttpServletRequest request) {
        log.debug("Getting trending polls with limit: {}", limit);

        Specification<Poll> trendingSpec = pollFilterMapper.createTrendingPollsSpecification(24);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("totalVotes").descending());

        Page<Poll> trendingPolls = pollRepository.findAll(trendingSpec, pageable);
        User currentUser = getOptionalUser(request);

        return trendingPolls.getContent().stream()
                .map(poll -> {
                    PollVote userVote = getUserVoteForPoll(poll, currentUser);
                    return pollQueryMapper.toPollListResponse(poll, userVote);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PollListResponse> getRecentPolls(int limit, HttpServletRequest request) {
        log.debug("Getting recent polls with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<Poll> recentPolls = pollRepository.findAll(pageable);
        User currentUser = getOptionalUser(request);

        return recentPolls.getContent().stream()
                .map(poll -> {
                    PollVote userVote = getUserVoteForPoll(poll, currentUser);
                    return pollQueryMapper.toPollListResponse(poll, userVote);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PollListResponse> getExpiringPolls(int hoursUntilExpiry, int limit, HttpServletRequest request) {
        log.debug("Getting expiring polls: {} hours, limit: {}", hoursUntilExpiry, limit);

        Specification<Poll> expiringSpec = pollFilterMapper.createExpiringPollsSpecification(hoursUntilExpiry);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("endDate").ascending());

        Page<Poll> expiringPolls = pollRepository.findAll(expiringSpec, pageable);
        User currentUser = getOptionalUser(request);

        return expiringPolls.getContent().stream()
                .map(poll -> {
                    PollVote userVote = getUserVoteForPoll(poll, currentUser);
                    return pollQueryMapper.toPollListResponse(poll, userVote);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPollStatistics(PollStatsRequest statsRequest, HttpServletRequest request) {
        log.debug("Getting poll statistics with request: {}", statsRequest);

        Specification<Poll> spec = pollFilterMapper.createStatsSpecification(statsRequest);
        List<Poll> polls = pollRepository.findAll(spec);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalPolls", polls.size());
        statistics.put("totalVotes", polls.stream().mapToLong(Poll::getTotalVotes).sum());
        statistics.put("totalOpinions", polls.stream().mapToLong(Poll::getTotalOpinions).sum());
        statistics.put("averageVotesPerPoll", polls.isEmpty() ? 0 :
                polls.stream().mapToLong(Poll::getTotalVotes).average().orElse(0));

        Map<PollStatus, Long> statusCounts = polls.stream()
                .collect(Collectors.groupingBy(Poll::getStatus, Collectors.counting()));
        statistics.put("pollsByStatus", statusCounts);

        Map<PollCategory, Long> categoryCounts = polls.stream()
                .collect(Collectors.groupingBy(Poll::getCategory, Collectors.counting()));
        statistics.put("pollsByCategory", categoryCounts);

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPollParticipationStats(Long pollId, HttpServletRequest request) {
        log.debug("Getting participation stats for poll ID: {}", pollId);

        Poll poll = getPollById(pollId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVotes", poll.getTotalVotes());
        stats.put("yesVotes", poll.getYesVotes());
        stats.put("noVotes", poll.getNoVotes());
        stats.put("neutralVotes", poll.getNeutralVotes());
        stats.put("yesPercentage", poll.getYesPercentage());
        stats.put("noPercentage", poll.getNoPercentage());
        stats.put("neutralPercentage", poll.getNeutralPercentage());
        stats.put("totalOpinions", poll.getTotalOpinions());

        long anonymousVotes = pollVoteRepository.countByPollAndIsAnonymousTrue(poll);
        long registeredVotes = pollVoteRepository.countByPollAndIsAnonymousFalse(poll);
        stats.put("anonymousVotes", anonymousVotes);
        stats.put("registeredVotes", registeredVotes);

        long anonymousOpinions = pollOpinionRepository.countByPollAndIsAnonymousTrue(poll);
        long registeredOpinions = pollOpinionRepository.countByPollAndIsAnonymousFalse(poll);
        stats.put("anonymousOpinions", anonymousOpinions);
        stats.put("registeredOpinions", registeredOpinions);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserViewPoll(Long pollId, HttpServletRequest request) {
        try {
            Poll poll = getPollById(pollId);
            return true;
        } catch (Exception e) {
            log.debug("User cannot view poll {}: {}", pollId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PollListResponse> getArchivedPolls(Pageable pageable, HttpServletRequest request) {
        log.debug("Getting archived polls");

        Page<Poll> archivedPolls = pollRepository.findByStatusOrderByCreatedAtDesc(PollStatus.ARCHIVED, pageable);
        User currentUser = getOptionalUser(request);

        return archivedPolls.map(poll -> {
            PollVote userVote = getUserVoteForPoll(poll, currentUser);
            return pollQueryMapper.toPollListResponse(poll, userVote);
        });
    }


    private Poll getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found with ID: " + pollId));
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    private User getOptionalUser(HttpServletRequest request) {
        try {
            return getAuthenticatedUser(request);
        } catch (Exception e) {
            return null;
        }
    }

    private PollVote getUserVoteForPoll(Poll poll, User user) {
        if (user == null) {
            return null;
        }
        return pollVoteRepository.findByPollAndUser(poll, user).orElse(null);
    }

    private boolean checkUserHasOpinion(Poll poll, User user) {
        if (user == null) {
            return false;
        }
        return pollOpinionRepository.existsByPollAndAuthor(poll, user);
    }

}
