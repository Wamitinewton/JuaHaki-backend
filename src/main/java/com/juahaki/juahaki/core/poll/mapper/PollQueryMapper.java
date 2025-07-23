package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollDetailsResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollListResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollSummaryResponse;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollAttachment;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.shared.enums.PollStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for poll query and result operations.
 * Handles conversion between poll entities and result/summary DTOs for various views.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollQueryMapper {

    /**
     * Convert Poll entity to PollListResponse for list views.
     *
     * @param poll     the poll entity
     * @param userVote the current user's vote (null if not voted)
     * @return poll list response DTO
     */
    public PollListResponse toPollListResponse(Poll poll, PollVote userVote) {
        if (poll == null) {
            return null;
        }

        return PollListResponse.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .category(poll.getCategory())
                .status(poll.getStatus())
                .isActive(poll.isActive())
                .startDate(poll.getStartDate())
                .endDate(poll.getEndDate())
                .createdAt(poll.getCreatedAt())
                .creatorUsername(poll.getCreator() != null ? poll.getCreator().getUsername() : "Unknown")
                .totalVotes(poll.getTotalVotes())
                .totalOpinions(poll.getTotalOpinions())
                .yesPercentage(poll.getYesPercentage())
                .userHasVoted(userVote != null)
                .build();
    }

    /**
     * Convert Poll entity to PollSummaryResponse for detailed summary views.
     *
     * @param poll           the poll entity
     * @param userVote       the current user's vote (null if not voted)
     * @param userHasOpinion whether the current user has submitted an opinion
     * @return poll summary response DTO
     */
    public PollSummaryResponse toPollSummaryResponse(Poll poll, PollVote userVote, boolean userHasOpinion) {
        if (poll == null) {
            return null;
        }

        PollSummaryResponse.CreatorInfo creatorInfo = buildCreatorInfo(poll);
        PollSummaryResponse.VotingResults votingResults = buildVotingResults(poll);
        PollSummaryResponse.OpinionsSummary opinionsSummary = buildOpinionsSummary(poll);
        PollSummaryResponse.UserParticipation userParticipation = buildUserParticipation(userVote, userHasOpinion);
        List<PollSummaryResponse.AttachmentInfo> attachmentInfos = buildAttachmentInfos(poll);

        return PollSummaryResponse.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .category(poll.getCategory())
                .status(poll.getStatus())
                .isActive(poll.isActive())
                .isExpired(poll.isExpired())
                .startDate(poll.getStartDate())
                .endDate(poll.getEndDate())
                .createdAt(poll.getCreatedAt())
                .creator(creatorInfo)
                .votingResults(votingResults)
                .opinionsSummary(opinionsSummary)
                .userParticipation(userParticipation)
                .attachments(attachmentInfos)
                .build();
    }

    /**
     * Convert Poll entity and additional data to PollDetailsResponse for full detail views.
     *
     * @param poll               the poll entity
     * @param userVote           the current user's vote (null if not voted)
     * @param userHasOpinion     whether the current user has submitted an opinion
     * @param topOpinions        list of top-rated opinions
     * @param recentOpinions     list of recent opinions
     * @param totalOpinionsCount total number of opinions
     * @return poll details response DTO
     */
    public PollDetailsResponse toPollDetailsResponse(Poll poll, PollVote userVote, boolean userHasOpinion,
                                                     List<OpinionResponse> topOpinions,
                                                     List<OpinionResponse> recentOpinions,
                                                     int totalOpinionsCount) {
        if (poll == null) {
            return null;
        }

        PollSummaryResponse pollSummary = toPollSummaryResponse(poll, userVote, userHasOpinion);

        boolean canVote = determineCanVote(poll, userVote);
        boolean canComment = determineCanComment(poll);

        return PollDetailsResponse.builder()
                .poll(pollSummary)
                .topOpinions(topOpinions != null ? topOpinions : List.of())
                .recentOpinions(recentOpinions != null ? recentOpinions : List.of())
                .totalOpinionsCount(totalOpinionsCount)
                .canVote(canVote)
                .canComment(canComment)
                .build();
    }

    /**
     * Build creator information for poll responses.
     *
     * @param poll the poll entity
     * @return creator info DTO
     */
    private PollSummaryResponse.CreatorInfo buildCreatorInfo(Poll poll) {
        if (poll.getCreator() == null) {
            return PollSummaryResponse.CreatorInfo.builder()
                    .username("Unknown")
                    .firstName("Unknown")
                    .build();
        }

        return PollSummaryResponse.CreatorInfo.builder()
                .username(poll.getCreator().getUsername())
                .firstName(poll.getCreator().getFirstName())
                .build();
    }

    /**
     * Build voting results summary for poll responses.
     *
     * @param poll the poll entity
     * @return voting results DTO
     */
    private PollSummaryResponse.VotingResults buildVotingResults(Poll poll) {
        return PollSummaryResponse.VotingResults.builder()
                .totalVotes(poll.getTotalVotes())
                .yesVotes(poll.getYesVotes())
                .noVotes(poll.getNoVotes())
                .neutralVotes(poll.getNeutralVotes())
                .yesPercentage(poll.getYesPercentage())
                .noPercentage(poll.getNoPercentage())
                .neutralPercentage(poll.getNeutralPercentage())
                .build();
    }

    /**
     * Build opinions summary for poll responses.
     * Note: This method assumes opinion counts by stance would be calculated elsewhere
     *
     * @param poll the poll entity
     * @return opinions summary DTO
     */
    private PollSummaryResponse.OpinionsSummary buildOpinionsSummary(Poll poll) {
        return PollSummaryResponse.OpinionsSummary.builder()
                .totalOpinions(poll.getTotalOpinions())
                .yesOpinions(0L)
                .noOpinions(0L)
                .neutralOpinions(0L)
                .build();
    }

    /**
     * Build user participation information for poll responses.
     *
     * @param userVote       the user's vote (null if not voted)
     * @param userHasOpinion whether the user has submitted an opinion
     * @return user participation DTO
     */
    private PollSummaryResponse.UserParticipation buildUserParticipation(PollVote userVote, boolean userHasOpinion) {
        return PollSummaryResponse.UserParticipation.builder()
                .hasVoted(userVote != null)
                .userVote(userVote != null ? userVote.getVoteChoice() : null)
                .hasOpinion(userHasOpinion)
                .build();
    }

    /**
     * Build attachment information list for poll responses.
     *
     * @param poll the poll entity
     * @return list of attachment info DTOs
     */
    private List<PollSummaryResponse.AttachmentInfo> buildAttachmentInfos(Poll poll) {
        if (poll.getAttachments() == null || poll.getAttachments().isEmpty()) {
            return List.of();
        }

        return poll.getAttachments().stream()
                .map(this::mapAttachmentToInfo)
                .collect(Collectors.toList());
    }

    /**
     * Map PollAttachment entity to AttachmentInfo DTO.
     *
     * @param attachment the attachment entity
     * @return attachment info DTO
     */
    private PollSummaryResponse.AttachmentInfo mapAttachmentToInfo(PollAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return PollSummaryResponse.AttachmentInfo.builder()
                .id(attachment.getId())
                .fileName(attachment.getFilaName()) // Note: Using existing field name from entity
                .fileUrl(attachment.getFileUrl())
                .attachmentType(attachment.getAttachmentType().name())
                .fileSize(attachment.getFileSize())
                .build();
    }

    /**
     * Determine if a user can vote on a poll.
     *
     * @param poll     the poll entity
     * @param userVote the user's existing vote (null if not voted)
     * @return true if the user can vote
     */
    private boolean determineCanVote(Poll poll, PollVote userVote) {
        // User can vote if:
        // 1. Poll is active
        // 2. Poll hasn't expired
        // 3. User hasn't voted yet
        return poll.isActive() &&
                !poll.isExpired() &&
                userVote == null &&
                poll.getStatus() == PollStatus.ACTIVE;
    }

    /**
     * Determine if a user can comment/submit opinions on a poll.
     *
     * @param poll the poll entity
     * @return true if comments are allowed
     */
    private boolean determineCanComment(Poll poll) {
        // User can comment if:
        // 1. Poll allows opinions
        // 2. Poll is active
        // 3. Poll hasn't expired
        return poll.getAllowOpinions() &&
                poll.isActive() &&
                !poll.isExpired() &&
                poll.getStatus() == PollStatus.ACTIVE;
    }

    /**
     * Create a minimal poll list response for basic poll information.
     *
     * @param poll the poll entity
     * @return minimal poll list response
     */
    public PollListResponse toMinimalPollListResponse(Poll poll) {
        if (poll == null) {
            return null;
        }

        return PollListResponse.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .category(poll.getCategory())
                .status(poll.getStatus())
                .isActive(poll.isActive())
                .createdAt(poll.getCreatedAt())
                .creatorUsername(poll.getCreator() != null ? poll.getCreator().getUsername() : "Unknown")
                .totalVotes(poll.getTotalVotes())
                .totalOpinions(poll.getTotalOpinions())
                .yesPercentage(poll.getYesPercentage())
                .userHasVoted(false)
                .build();
    }

}
