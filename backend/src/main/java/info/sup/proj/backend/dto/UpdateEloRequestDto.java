package info.sup.proj.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateEloRequestDto {
    private Long userId;
    private Integer scoreToAdd;

}