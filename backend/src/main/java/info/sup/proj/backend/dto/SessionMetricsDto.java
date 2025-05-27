package info.sup.proj.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMetricsDto {
    private Integer attemptCount;
    private Integer bestInteractionCount;
    private Long bestTimeSeconds;
    private Boolean isCompleted;
    private Integer currentInteractionCount;
    
    private Integer totalScore;
    private Boolean hasFailed;
    private Integer timeScore;
    private Integer efficiencyScore;
    private Integer tokenScore;
    private Integer correctnessScore;
    private Integer codeQualityScore;
    private Long timeSeconds;
    private Integer interactionCount;
}