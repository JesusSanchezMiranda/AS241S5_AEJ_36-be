package practice.SpringWebflux.service;

import org.springframework.http.codec.multipart.FilePart;

import practice.SpringWebflux.model.AiImageResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiImageService {

    Mono<AiImageResult> colorizePhoto(FilePart file, String name, String description);

    Mono<AiImageResult> removeBackground(FilePart file, String name, String description);

    Flux<AiImageResult> getAllResults();

    Flux<AiImageResult> getArchivedByApi(String apiName);

    Flux<AiImageResult> getResultsByApi(String apiName);

    Flux<AiImageResult> getActiveByApi(String apiName);

    Mono<AiImageResult> updateNameAndDescription(Long id, String name, String description);

    Mono<AiImageResult> archive(Long id);

    Mono<AiImageResult> unarchive(Long id);

    Mono<Void> delete(Long id);
}
