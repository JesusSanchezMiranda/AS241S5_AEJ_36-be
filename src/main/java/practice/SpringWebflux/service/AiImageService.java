package practice.SpringWebflux.service;

import org.springframework.http.codec.multipart.FilePart;

import practice.SpringWebflux.model.AiImageResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiImageService {
    Mono<AiImageResult> colorizePhoto(FilePart file);  

    Mono<AiImageResult> removeBackground(FilePart file);

    Flux<AiImageResult> getAllResults();

    Flux<AiImageResult> getResultsByApi(String apiName);
}
