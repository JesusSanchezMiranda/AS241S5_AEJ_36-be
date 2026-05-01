package practice.SpringWebflux.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import practice.SpringWebflux.model.AiImageResult;
import reactor.core.publisher.Flux;

public interface AiImageResultRepository extends ReactiveCrudRepository<AiImageResult, Long> {
    Flux<AiImageResult> findByApiName(String apiName);
    Flux<AiImageResult> findByApiNameAndArchived(String apiName, Boolean archived);
    Flux<AiImageResult> findByArchived(Boolean archived);
    
}