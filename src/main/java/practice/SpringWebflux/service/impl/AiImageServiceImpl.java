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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiImageServiceImpl implements AiImageService {
    private final AiImageResultRepository repository;
    private final WebClient webClient;

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    // Primer API colorizacion de Imagenes
    @Override
    public Mono<AiImageResult> colorizePhoto(FilePart file) {

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] imageBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(imageBytes);
                    DataBufferUtils.release(dataBuffer);

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
                            .uri("https://colorize-photo1.p.rapidapi.com/colorize_image_with_auto_prompt")
                            .header("x-rapidapi-key", rapidApiKey)
                            .header("x-rapidapi-host", "colorize-photo1.p.rapidapi.com")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .bodyValue(builder.build())
                            .retrieve()
                            .bodyToMono(byte[].class); // ← recibir como bytes, no String
                })
                .flatMap(imageResultBytes -> {
                    // Guardar el archivo en disco y devolver la ruta
                    String filename = "colorized_" + System.currentTimeMillis() + ".jpg";
                    String outputPath = "output/" + filename;

                    try {
                        // Crear carpeta si no existe
                        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("output"));
                        // Guardar imagen en disco
                        java.nio.file.Files.write(java.nio.file.Paths.get(outputPath), imageResultBytes);
                        System.out.println(">>> IMAGEN GUARDADA EN: " + outputPath);
                    } catch (Exception e) {
                        System.out.println(">>> ERROR GUARDANDO: " + e.getMessage());
                    }

                    AiImageResult result = new AiImageResult();
                    result.setApiName("colorize");
                    result.setInputUrl(file.filename());
                    result.setOutputUrl(outputPath); // ← ruta local del archivo
                    result.setStatus("SUCCESS");
                    result.setCreatedAt(LocalDateTime.now());
                    return repository.save(result);
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
                    error.setCreatedAt(LocalDateTime.now());
                    return repository.save(error);
                });
    }

    @Override
    public Mono<AiImageResult> removeBackground(FilePart file) {

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] imageBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(imageBytes);
                    DataBufferUtils.release(dataBuffer);

                    System.out.println(">>> PASO 1 - Imagen leída, bytes: " + imageBytes.length);

                    MultipartBodyBuilder builder = new MultipartBodyBuilder();
                    builder.part("image", imageBytes)
                            .filename(file.filename())
                            .contentType(MediaType.IMAGE_JPEG);
                    builder.part("model", "falcon");

                    System.out.println(">>> PASO 2 - Llamando a RapidAPI...");

                    return webClient.post()
                            .uri("https://ai-background-remover.p.rapidapi.com/image/matte/v1")
                            .header("x-rapidapi-key", rapidApiKey)
                            .header("x-rapidapi-host", "ai-background-remover.p.rapidapi.com")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .bodyValue(builder.build())
                            .retrieve()
                            .toEntity(byte[].class) // ← cambiado para ver headers + body
                            .doOnNext(entity -> {
                                System.out.println(">>> PASO 3 - Status: " + entity.getStatusCode());
                                System.out
                                        .println(">>> PASO 3 - Content-Type: " + entity.getHeaders().getContentType());
                                byte[] body = entity.getBody();
                                if (body != null) {
                                    System.out.println(">>> PASO 3 - Bytes recibidos: " + body.length);
                                    // ver si empieza con JPEG, PNG o JSON
                                    String preview = new String(body, 0, Math.min(50, body.length));
                                    System.out.println(">>> PASO 3 - Preview: " + preview);
                                } else {
                                    System.out.println(">>> PASO 3 - Body es NULL");
                                }
                            })
                            .map(entity -> entity.getBody());
                })
                .flatMap(imageResultBytes -> {
                    System.out.println(">>> PASO 4 - Guardando imagen, bytes: " + imageResultBytes.length);

                    String filename = "removebg_" + System.currentTimeMillis() + ".png";
                    String outputPath = "output/" + filename;

                    try {
                        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("output"));
                        java.nio.file.Files.write(java.nio.file.Paths.get(outputPath), imageResultBytes);
                        System.out.println(">>> PASO 4 - Imagen guardada en: " + outputPath);
                    } catch (Exception e) {
                        System.out.println(">>> PASO 4 - Error guardando: " + e.getMessage());
                    }

                    AiImageResult result = new AiImageResult();
                    result.setApiName("remove-bg");
                    result.setInputUrl(file.filename());
                    result.setOutputUrl(outputPath);
                    result.setStatus("SUCCESS");
                    result.setCreatedAt(LocalDateTime.now());
                    return repository.save(result);
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
                    error.setCreatedAt(LocalDateTime.now());
                    return repository.save(error);
                });
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
}
