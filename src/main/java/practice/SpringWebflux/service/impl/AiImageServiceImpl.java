package practice.SpringWebflux.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import practice.SpringWebflux.model.AiImageResult;
import practice.SpringWebflux.repository.AiImageResultRepository;
import practice.SpringWebflux.service.AiImageService;
import practice.SpringWebflux.service.CloudinaryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiImageServiceImpl implements AiImageService {
    private final AiImageResultRepository repository;
    private final WebClient webClient;
    private final CloudinaryService cloudinaryService;

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.colorize.url}")
    private String colorizeUrl;

    @Value("${rapidapi.colorize.host}")
    private String colorizeHost;

    @Value("${rapidapi.remove-bg.url}")
    private String removeBgUrl;

    @Value("${rapidapi.remove-bg.host}")
    private String removeBgHost;

    // Primer API colorizacion de Imagenes
    @Override
    public Mono<AiImageResult> colorizePhoto(FilePart file, String name, String description) {

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] imageBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(imageBytes);
                    DataBufferUtils.release(dataBuffer);

                    String inputPublicId = "input_" + System.currentTimeMillis() + "_" + file.filename();

                    // Subir input a Cloudinary
                    return cloudinaryService.upload(imageBytes, "input", inputPublicId)
                            .flatMap(inputUrl -> {

                                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                builder.part("image", imageBytes)
                                        .filename(file.filename())
                                        .contentType(MediaType.IMAGE_JPEG);
                                builder.part("temperature", "-0.1");
                                builder.part("raw_captions", "false");
                                builder.part("standard_filter_id", "1");
                                builder.part("white_balance", "false");
                                builder.part("resolution", "watermarked-sd");
                                builder.part("auto_color", "true");
                                builder.part("artistic_filter_id", "0");
                                builder.part("saturation", "1.1");

                                return webClient.post()
                                        .uri(colorizeUrl)
                                        .header("x-rapidapi-key", rapidApiKey)
                                        .header("x-rapidapi-host", colorizeHost)
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .bodyValue(builder.build())
                                        .retrieve()
                                        .bodyToMono(byte[].class)
                                        .flatMap(outputBytes -> {
                                            String outputPublicId = "colorized_" + System.currentTimeMillis();

                                            // Subir output a Cloudinary
                                            return cloudinaryService.upload(outputBytes, "output", outputPublicId)
                                                    .flatMap(outputUrl -> {
                                                        AiImageResult result = new AiImageResult();
                                                        result.setApiName("colorize");
                                                        result.setInputUrl(file.filename());
                                                        result.setInputSavedUrl(inputUrl);
                                                        result.setName(name);
                                                        result.setDescription(description);
                                                        result.setOutputUrl(outputUrl);
                                                        result.setStatus("SUCCESS");
                                                        result.setArchived(false);
                                                        result.setCreatedAt(LocalDateTime.now());
                                                        return repository.save(result);
                                                    });
                                        });
                            });
                })
                .onErrorResume(ex -> {
                    if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcEx) {
                        System.out.println(">>> ERROR BODY: " + wcEx.getResponseBodyAsString());
                    } else {
                        System.out.println(">>> ERROR REAL: " + ex.getMessage());
                    }
                    AiImageResult error = new AiImageResult();
                    error.setApiName("colorize");
                    error.setInputUrl(file.filename());
                    error.setOutputUrl(null);
                    error.setStatus("ERROR");
                    error.setArchived(false);
                    error.setCreatedAt(LocalDateTime.now());
                    return repository.save(error);
                });
    }

    @Override
    public Mono<AiImageResult> removeBackground(FilePart file, String name, String description) {

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] imageBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(imageBytes);
                    DataBufferUtils.release(dataBuffer);

                    System.out.println(">>> PASO 1 - Imagen leída, bytes: " + imageBytes.length);

                    String inputPublicId = "input_" + System.currentTimeMillis() + "_" + file.filename();

                    // Subir input a Cloudinary
                    return cloudinaryService.upload(imageBytes, "input", inputPublicId)
                            .flatMap(inputUrl -> {

                                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                builder.part("image", imageBytes)
                                        .filename(file.filename())
                                        .contentType(MediaType.IMAGE_JPEG);
                                builder.part("model", "falcon");

                                System.out.println(">>> PASO 2 - Llamando a RapidAPI...");

                                return webClient.post()
                                        .uri(removeBgUrl)
                                        .header("x-rapidapi-key", rapidApiKey)
                                        .header("x-rapidapi-host", removeBgHost)
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .bodyValue(builder.build())
                                        .retrieve()
                                        .toEntity(byte[].class)
                                        .doOnNext(entity -> {
                                            System.out.println(">>> PASO 3 - Status: " + entity.getStatusCode());
                                            System.out.println(">>> PASO 3 - Content-Type: "
                                                    + entity.getHeaders().getContentType());
                                            byte[] body = entity.getBody();
                                            if (body != null) {
                                                System.out.println(">>> PASO 3 - Bytes recibidos: " + body.length);
                                                String preview = new String(body, 0, Math.min(50, body.length));
                                                System.out.println(">>> PASO 3 - Preview: " + preview);
                                            } else {
                                                System.out.println(">>> PASO 3 - Body es NULL");
                                            }
                                        })
                                        .flatMap(entity -> {
                                            byte[] imageResultBytes = entity.getBody();
                                            System.out.println(">>> PASO 4 - Subiendo output a Cloudinary...");

                                            String outputPublicId = "removebg_" + System.currentTimeMillis();

                                            // Subir output a Cloudinary
                                            return cloudinaryService.upload(imageResultBytes, "output", outputPublicId)
                                                    .flatMap(outputUrl -> {
                                                        AiImageResult result = new AiImageResult();
                                                        result.setApiName("remove-bg");
                                                        result.setName(name);
                                                        result.setDescription(description);
                                                        result.setInputUrl(file.filename());
                                                        result.setInputSavedUrl(inputUrl);
                                                        result.setOutputUrl(outputUrl);
                                                        result.setStatus("SUCCESS");
                                                        result.setArchived(false);
                                                        result.setCreatedAt(LocalDateTime.now());
                                                        return repository.save(result);
                                                    });
                                        });
                            });
                })
                .onErrorResume(ex -> {
                    System.out.println(">>> FALLO EN PASO: " + ex.getClass().getSimpleName());
                    System.out.println(">>> MENSAJE: " + ex.getMessage());
                    if (ex.getCause() != null) {
                        System.out.println(">>> CAUSA RAIZ: " + ex.getCause().getMessage());
                    }
                    if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcEx) {
                        System.out.println(">>> HTTP STATUS: " + wcEx.getStatusCode());
                        System.out.println(">>> RESPONSE BODY: " + wcEx.getResponseBodyAsString());
                    }
                    AiImageResult error = new AiImageResult();
                    error.setApiName("remove-bg");
                    error.setInputUrl(file.filename());
                    error.setOutputUrl(null);
                    error.setStatus("ERROR");
                    error.setArchived(false);
                    error.setCreatedAt(LocalDateTime.now());
                    return repository.save(error);
                });
    }

    @Override
    public Flux<AiImageResult> getActiveByApi(String apiName) {
        return repository.findByApiNameAndArchived(apiName, false);
    }

    @Override
    public Mono<AiImageResult> updateNameAndDescription(Long id, String name, String description) {
        return repository.findById(id)
                .flatMap(result -> {
                    result.setName(name);
                    result.setDescription(description);
                    result.setUpdatedAt(LocalDateTime.now());
                    return repository.save(result);
                });
    }

    @Override
    public Mono<AiImageResult> archive(Long id) {
        return repository.findById(id)
                .flatMap(result -> {
                    result.setArchived(true);
                    result.setUpdatedAt(LocalDateTime.now());
                    return repository.save(result);
                });
    }

    @Override
    public Mono<AiImageResult> unarchive(Long id) {
        return repository.findById(id)
                .flatMap(result -> {
                    result.setArchived(false);
                    result.setUpdatedAt(LocalDateTime.now());
                    return repository.save(result);
                });
    }

    @Override
    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }

    // Consulta de los resultados a la BD
    @Override
    public Flux<AiImageResult> getAllResults() {
        return repository.findAll();
    }

    @Override
    public Flux<AiImageResult> getResultsByApi(String apiName) {
        return repository.findByApiName(apiName);
    }

    @Override
    public Flux<AiImageResult> getArchivedByApi(String apiName) {
        return repository.findByApiNameAndArchived(apiName, true);
    }
}
