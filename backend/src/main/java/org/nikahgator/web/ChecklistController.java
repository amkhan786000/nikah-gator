package org.nikahgator.web;

import java.util.List;
import java.util.Map;

import org.nikahgator.dto.ChecklistDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.ChecklistService;
import org.nikahgator.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig): checklist and documents of nikahs of the imam's own masjid. */
@RestController
@RequestMapping("/api/imam")
public class ChecklistController {

    private final ChecklistService checklist;
    private final DocumentService documents;

    public ChecklistController(ChecklistService checklist, DocumentService documents) {
        this.checklist = checklist;
        this.documents = documents;
    }

    @GetMapping("/checklist-progress")
    public List<Progress> progress(@AuthenticationPrincipal AppUser imam) {
        return checklist.progress(imam);
    }

    @GetMapping("/nikahs/{id}/checklist")
    public List<ItemDto> list(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return checklist.list(imam, id);
    }

    @PostMapping("/nikahs/{id}/checklist")
    public ItemDto add(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody NewItem body) {
        return checklist.add(imam, id, body);
    }

    @PutMapping("/nikahs/{id}/checklist/{itemId}")
    public ItemDto update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long itemId, @RequestBody ItemUpdate body) {
        return checklist.update(imam, id, itemId, body);
    }

    @DeleteMapping("/nikahs/{id}/checklist/{itemId}")
    public void delete(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long itemId) {
        checklist.delete(imam, id, itemId);
    }

    @GetMapping("/nikahs/{id}/documents")
    public List<DocumentDto> documents(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return documents.list(imam, id);
    }

    @PostMapping(value = "/nikahs/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto upload(@AuthenticationPrincipal AppUser imam, @PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(value = "checklistId", required = false) Long checklistId,
                              @RequestParam(value = "checklistCode", required = false) String checklistCode,
                              @RequestParam(value = "markDone", defaultValue = "true") boolean markDone) {
        return documents.upload(imam, id, file, checklistId, checklistCode, markDone);
    }

    @GetMapping("/nikahs/{id}/documents/{docId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long docId) {
        DocumentService.Download d = documents.download(imam, id, docId);
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store");
        if (d.size() >= 0) b.contentLength(d.size());
        return b.body(d.resource());
    }

    @DeleteMapping("/nikahs/{id}/documents/{docId}")
    public void deleteDocument(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long docId) {
        documents.delete(imam, id, docId);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "The file is larger than 25 MB"));
    }
}
