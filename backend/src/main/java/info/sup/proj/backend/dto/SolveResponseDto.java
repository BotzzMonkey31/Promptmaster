package info.sup.proj.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SolveResponseDto {
    private String text;
    private String code;
    private String completeCode;

    public SolveResponseDto(String text, String code, String completeCode) {
        this.text = text;
        this.code = code;
        this.completeCode = completeCode;
    }

}