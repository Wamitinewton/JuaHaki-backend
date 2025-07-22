package com.juahaki.juahaki.core.poll.dto.opinions;

import com.juahaki.juahaki.shared.enums.VoteChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitOpinionRequest {

    @NotNull(message = "Poll ID is required")
    private Long pollId;

    @NotBlank(message = "Opinion content is required")
    @Size(max = 2000, message = "Opinion must not exceed 2000 characters")
    private String content;

    @NotNull(message = "Stance is required")
    private VoteChoice stance;

    @Builder.Default
    private Boolean isAnonymous = false;

    private List<MultipartFile> attachments;
}
