package practice.SpringWebflux.rest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import practice.SpringWebflux.model.AiImageResult;
import practice.SpringWebflux.service.AiImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class AiImageRest {
    private final AiImageService aiImageService;

    @PostMapping(value = "/colorize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AiImageResult>> colorize(
            @RequestPart("file") FilePart file,
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "description", required = false) String description) {
        return aiImageService.colorizePhoto(file, name, description)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/remove-bg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AiImageResult>> removeBg(
            @RequestPart("file") FilePart file,
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "description", required = false) String description) {
        return aiImageService.removeBackground(file, name, description)
                .map(ResponseEntity::ok);
    }

    // GET /api/images/results
    @GetMapping("/results")
    public Flux<AiImageResult> getAllResults() {
        return aiImageService.getAllResults();
    }

    // GET /api/images/results/colorize (o remove-bg)
    @GetMapping("/results/{apiName}")
    public Flux<AiImageResult> getByApi(@PathVariable String apiName) {
        return aiImageService.getResultsByApi(apiName);
    }

    // GET activos por api (no archivados)
    @GetMapping("/results/{apiName}/active")
    public Flux<AiImageResult> getActiveByApi(@PathVariable String apiName) {
        return aiImageService.getActiveByApi(apiName);
    }

    @GetMapping("/results/{apiName}/archived")
    public Flux<AiImageResult> getArchivedByApi(@PathVariable String apiName) {
        return aiImageService.getArchivedByApi(apiName);
    }

    // PATCH actualizar nombre y descripción
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<AiImageResult>> update(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        return aiImageService.updateNameAndDescription(id, body.get("name"), body.get("description"))
                .map(ResponseEntity::ok);
    }

    // PATCH archivar
    @PatchMapping("/{id}/archive")
    public Mono<ResponseEntity<AiImageResult>> archive(@PathVariable Long id) {
        return aiImageService.archive(id)
                .map(ResponseEntity::ok);
    }

    // PATCH desarchivar
    @PatchMapping("/{id}/unarchive")
    public Mono<ResponseEntity<AiImageResult>> unarchive(@PathVariable Long id) {
        return aiImageService.unarchive(id)
                .map(ResponseEntity::ok);
    }

    // DELETE físico
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return aiImageService.delete(id)
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

}
