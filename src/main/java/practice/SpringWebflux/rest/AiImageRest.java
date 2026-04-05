package practice.SpringWebflux.rest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    // POST /api/ai-images/colorize (form-data con campo "file")
    @PostMapping(value = "/colorize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AiImageResult>> colorize(
            @RequestPart("file") FilePart file) {
        return aiImageService.colorizePhoto(file)
                .map(ResponseEntity::ok);
    }

    // POST /api/ai-images/remove-bg (form-data con campo "file")
    @PostMapping(value = "/remove-bg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<AiImageResult>> removeBg(
            @RequestPart("file") FilePart file) {
        return aiImageService.removeBackground(file)
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

    @GetMapping("/output/{filename}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable String filename) {
        var file = new FileSystemResource("output/" + filename);

        if (!file.exists()) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        // detecta el tipo según extensión
        String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body((Resource) file));
    }
}
