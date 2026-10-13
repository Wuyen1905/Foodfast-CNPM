package com.foodfast.controller;

import com.foodfast.dto.CompleteRefundRequest;
import com.foodfast.dto.ComplaintResponse;
import com.foodfast.dto.CreateComplaintRequest;
import com.foodfast.dto.ReviewComplaintRequest;
import com.foodfast.entity.ComplaintStatus;
import com.foodfast.service.ComplaintService;
import com.foodfast.service.ComplaintEvidenceService;
import com.foodfast.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final ComplaintService complaintService;
    private final ComplaintEvidenceService evidenceService;
    private final CurrentUserService currentUserService;

    public ComplaintController(
            ComplaintService complaintService,
            ComplaintEvidenceService evidenceService,
            CurrentUserService currentUserService
    ) {
        this.complaintService = complaintService;
        this.evidenceService = evidenceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadEvidence(
            @RequestPart("file") MultipartFile file
    ) {
        currentUserService.requireUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "url", evidenceService.store(file)
        ));
    }

    @GetMapping("/evidence/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> evidence(
            @PathVariable String fileName
    ) {
        currentUserService.requireUser();
        ComplaintEvidenceService.StoredEvidence stored = evidenceService.load(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .body(stored.resource());
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> create(@RequestBody @Valid CreateComplaintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.create(
                currentUserService.requireUser().getId(),
                request
        ));
    }

    @GetMapping
    public List<ComplaintResponse> list(@RequestParam(required = false) ComplaintStatus status) {
        return complaintService.list(
                currentUserService.role(),
                currentUserService.businessActorId(),
                status
        );
    }

    @PatchMapping("/{complaintId}/review")
    public ComplaintResponse review(
            @PathVariable String complaintId,
            @RequestBody @Valid ReviewComplaintRequest request
    ) {
        return complaintService.review(
                complaintId,
                currentUserService.role(),
                currentUserService.businessActorId(),
                request
        );
    }

    @PatchMapping("/{complaintId}/refund/complete")
    public ComplaintResponse completeRefund(
            @PathVariable String complaintId,
            @RequestBody @Valid CompleteRefundRequest request
    ) {
        return complaintService.completeRefund(
                complaintId,
                currentUserService.requireUser().getId(),
                request
        );
    }
}
