package io.metersphere.functional.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author wx
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class FileDumpRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目id",requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @Schema(description = "用例id",requiredMode = Schema.RequiredMode.REQUIRED)
    private String caseId;

    @Schema(description = "文件id",requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileId;


}
