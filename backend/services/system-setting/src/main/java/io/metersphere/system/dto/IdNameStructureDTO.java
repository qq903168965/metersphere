package io.metersphere.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdNameStructureDTO {
    @Schema(description =  "选项ID")
    private String id;
    @Schema(description =  "选项名称")
    private String name;

}