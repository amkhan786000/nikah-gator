package org.nikahgator.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.nikahgator.dto.ChecklistDtos.DocumentDto;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.ChecklistItem;
import org.nikahgator.entity.Document;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Uploaded documents of a nikah, stored under {upload-dir}/{masjidId}/{nikahId}/{random}-{sanitized name}. */
@Service
public class DocumentService {

    public static final long MAX_BYTES = 25L * 1024 * 1024;

    private final DocumentRepository documents;
    private final ChecklistService checklist;
    private final Path root;

    public DocumentService(DocumentRepository documents, ChecklistService checklist, @Value("${app.upload-dir}") String uploadDir) {
        this.documents = documents;
        this.checklist = checklist;
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<DocumentDto> list(AppUser imam, Long nikahId) {
        checklist.nikahOf(imam, nikahId);
        return documents.findByRequestIdOrderByIdDesc(nikahId).stream().map(DocumentService::toDto).toList();
    }

    @Transactional
    public DocumentDto upload(AppUser imam, Long nikahId, MultipartFile file, Long checklistId, boolean markDone) {
        checklist.nikahOf(imam, nikahId);
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a file to upload");
        if (file.getSize() > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "The file is larger than 25 MB");
        ChecklistItem item = checklistId == null ? null : checklist.find(imam, nikahId, checklistId);

        String name = sanitize(file.getOriginalFilename());
        String type;
        try (InputStream in = file.getInputStream()) {
            type = detectType(in.readNBytes(8), name);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file could not be read");
        }

        Path dir = root.resolve(imam.getMasjidId().toString()).resolve(nikahId.toString()).normalize();
        Path target = dir.resolve(UUID.randomUUID().toString().replace("-", "") + "-" + name).normalize();
        if (!target.startsWith(root)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be saved");
        }

        Document d = new Document();
        d.setRequestId(nikahId);
        d.setChecklistId(item == null ? null : item.getId());
        d.setFileName(name);
        d.setStoredPath(root.relativize(target).toString());
        d.setContentType(type);
        d.setSizeBytes(file.getSize());
        d.setUploadedBy(imam.getId());
        try {
            d = documents.save(d);
            if (item != null && markDone) checklist.markDone(item);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
        return toDto(d);
    }

    public record Download(Resource resource, String fileName, String contentType, long size) {}

    public Download download(AppUser imam, Long nikahId, Long docId) {
        Document d = find(imam, nikahId, docId);
        Path p = resolve(d);
        if (!Files.isReadable(p)) throw new ApiException(HttpStatus.NOT_FOUND, "The file is no longer available");
        return new Download(new FileSystemResource(p), d.getFileName(), d.getContentType(), d.getSizeBytes() == null ? -1 : d.getSizeBytes());
    }

    @Transactional
    public void delete(AppUser imam, Long nikahId, Long docId) {
        Document d = find(imam, nikahId, docId);
        documents.delete(d);
        deleteQuietly(resolve(d));
    }

    private Document find(AppUser imam, Long nikahId, Long docId) {
        checklist.nikahOf(imam, nikahId);
        return documents.findByIdAndRequestId(docId, nikahId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private Path resolve(Document d) {
        Path p = root.resolve(d.getStoredPath()).normalize();
        if (!p.startsWith(root)) throw new ApiException(HttpStatus.NOT_FOUND, "Document not found");
        return p;
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // an orphan file is harmless
        }
    }

    /** Type from the file's first bytes (the client's content type is not trusted); the extension must match it. */
    static String detectType(byte[] head, String name) {
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        String type = null;
        if (head.length >= 5 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F' && head[4] == '-') {
            type = "application/pdf";
        } else if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            type = "image/jpeg";
        } else if (head.length >= 8 && Arrays.equals(Arrays.copyOf(head, 8), new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            type = "image/png";
        }
        boolean extOk = switch (type == null ? "" : type) {
            case "application/pdf" -> ext.equals("pdf");
            case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
            case "image/png" -> ext.equals("png");
            default -> false;
        };
        if (!extOk) throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, JPG and PNG files are allowed");
        return type;
    }

    /** Last path segment only, safe characters only, bounded length. */
    static String sanitize(String original) {
        String n = original == null ? "" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").replaceAll("^\\.+", "").trim();
        if (n.length() > 100) {
            int dot = n.lastIndexOf('.');
            String ext = dot > 0 && n.length() - dot <= 6 ? n.substring(dot) : "";
            n = n.substring(0, 100 - ext.length()) + ext;
        }
        return n.isEmpty() ? "file" : n;
    }

    private static DocumentDto toDto(Document d) {
        return new DocumentDto(d.getId(), d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getChecklistId(), d.getCreatedAt());
    }
}
