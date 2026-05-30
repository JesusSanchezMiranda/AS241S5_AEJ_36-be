package practice.SpringWebflux.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    // Sube bytes a Cloudinary y devuelve la URL pública
    public Mono<String> upload(byte[] bytes, String folder, String publicId) {
        return Mono.fromCallable(() -> {
            Map result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "folder",    folder,
                "public_id", publicId,
                "overwrite", true
            ));
            return (String) result.get("secure_url");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // Elimina una imagen de Cloudinary por su publicId
    public Mono<Void> delete(String publicId) {
        return Mono.fromCallable(() -> {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
