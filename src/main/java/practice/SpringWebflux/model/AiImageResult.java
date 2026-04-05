package practice.SpringWebflux.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("ai_image_results")
public class AiImageResult {

    @Id
    private Long id;

    @Column("api_name")
    private String apiName;

    @Column("input_url")
    private String inputUrl;

    @Column("output_url")
    private String outputUrl;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;
    
}
