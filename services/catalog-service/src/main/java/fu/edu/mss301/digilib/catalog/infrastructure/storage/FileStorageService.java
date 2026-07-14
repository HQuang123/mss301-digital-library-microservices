package fu.edu.mss301.digilib.catalog.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> PDF_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "application/acrobat",
            "applications/vnd.pdf",
            "text/pdf",
            "text/x-pdf",
            "application/octet-stream"
    );

    private final Path uploadRoot;

    public FileStorageService(@Value("${catalog.upload-dir:uploads/files}") String uploadDir) {
        this.uploadRoot = UploadPathResolver.resolve(uploadDir);
    }

    @PostConstruct
    void logResolvedUploadRoot() {
        log.info("Catalog file storage initialized. cwd='{}', uploadRoot='{}'",
                Path.of("").toAbsolutePath().normalize(),
                uploadRoot.toAbsolutePath().normalize());
    }

    public String storeBookCover(MultipartFile file, Long bookId) {
        String extension = getExtension(file);
        String contentType = getContentType(file);

        if (!IMAGE_EXTENSIONS.contains(extension) || !IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed");
        }

        String filename = "book-" + bookId + "-cover-" + UUID.randomUUID() + "." + extension;
        store(file, uploadRoot.resolve("images"), filename);
        return "/files/images/" + filename;
    }

    public String storeDigitalResourcePdf(MultipartFile file, Long bookId) {
        String extension = getExtension(file);
        String contentType = getContentType(file);

        if (!"pdf".equals(extension)) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        if (!PDF_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported PDF content type: " + contentType);
        }

        String filename = "book-" + bookId + "-resource-" + UUID.randomUUID() + ".pdf";
        store(file, uploadRoot.resolve("resources"), filename);
        return "/files/resources/" + filename;
    }

    public Resource loadStoredFile(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Stored file path cannot be empty");
        }

        String normalizedPath = storedPath.trim();
        if (!normalizedPath.startsWith("/files/")) {
            throw new IllegalArgumentException("Unsupported stored file path: " + storedPath);
        }

        String relativePath = normalizedPath.substring("/files/".length());
        Path targetFile = uploadRoot.resolve(relativePath).normalize();

        if (!targetFile.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid stored file path");
        }

        try {
            if (!Files.exists(targetFile) || !Files.isReadable(targetFile)) {
                throw new IllegalArgumentException("Stored file not found");
            }

            return new UrlResource(targetFile.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read stored file", exception);
        }
    }

    public MediaType detectMediaType(Resource resource) {
        try {
          String contentType = Files.probeContentType(Path.of(resource.getURI()));
          if (contentType != null && !contentType.isBlank()) {
              return MediaType.parseMediaType(contentType);
          }
        } catch (Exception ignored) {
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public Resource loadBookCover(Long bookId, String storedPath) {
        if (storedPath != null && !storedPath.trim().isEmpty()) {
            try {
                return loadStoredFile(storedPath);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (bookId == null) {
            throw new IllegalArgumentException("Book ID is required to load cover image");
        }

        Path imagesDirectory = uploadRoot.resolve("images").normalize();
        if (!Files.exists(imagesDirectory)) {
            throw new IllegalArgumentException("Book cover directory not found");
        }

        String filenamePrefix = "book-" + bookId + "-cover-";

        try (Stream<Path> paths = Files.list(imagesDirectory)) {
            Optional<Path> latestFile = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filenamePrefix))
                    .max(Comparator.comparingLong(this::getLastModifiedSafe));

            Path targetFile = latestFile.orElseThrow(() -> new IllegalArgumentException("Book cover not found"));
            return new UrlResource(targetFile.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load book cover", exception);
        }
    }

    private void store(MultipartFile file, Path targetDirectory, String filename) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            Files.createDirectories(targetDirectory);
            Path targetFile = targetDirectory.resolve(filename).normalize();

            if (!targetFile.startsWith(targetDirectory)) {
                throw new IllegalArgumentException("Invalid file path");
            }

            log.info("Storing upload file. originalFilename='{}', contentType='{}', targetFile='{}'",
                    file.getOriginalFilename(),
                    file.getContentType(),
                    targetFile.toAbsolutePath().normalize());
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store file in " + targetDirectory.toAbsolutePath(), exception);
        }
    }

    private String getExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("File extension is required");
        }

        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String getContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("File content type is required");
        }

        return contentType.toLowerCase(Locale.ROOT);
    }

    private long getLastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }
}
