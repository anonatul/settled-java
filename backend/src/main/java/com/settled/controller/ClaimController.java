package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.common.PageResponse;
import com.settled.domain.User;
import com.settled.domain.enums.Role;
import com.settled.dto.claim.*;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.UserRepository;
import com.settled.service.ClaimDocumentService;
import com.settled.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Claim lifecycle: submit, review, decide, settle")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimDocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Submit a new claim (rate limited: 10/hour per user)")
    public ApiResponse<ClaimResponse> submit(@CurrentUser UUID userId,
                                             @Valid @RequestBody ClaimRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.ok(claimService.submit(userId, request, httpRequest));
    }

    @GetMapping
    @Operation(summary = "List my claims (customers) or my assigned claims (officers)")
    public ApiResponse<PageResponse<ClaimResponse>> list(@CurrentUser UUID userId,
                                                         @RequestParam(required = false) String status,
                                                         @PageableDefault(size = 10) Pageable pageable,
                                                         Authentication authentication) {
        User user = loadUser(userId);
        if (user.getRole() == Role.CUSTOMER) {
            return ApiResponse.ok(claimService.listMine(userId, status, pageable));
        }
        return ApiResponse.ok(claimService.listForOfficer(userId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a claim detail")
    public ApiResponse<ClaimResponse> get(@CurrentUser UUID userId, @PathVariable UUID id,
                                          Authentication authentication) {
        User user = loadUser(userId);
        if (user.getRole() == Role.CUSTOMER) {
            return ApiResponse.ok(claimService.getMine(userId, id));
        }
        if (user.getRole() == Role.CLAIM_OFFICER) {
            return ApiResponse.ok(claimService.getForOfficer(userId, id));
        }
        return ApiResponse.ok(claimService.getAny(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get claim status history")
    public ApiResponse<List<StatusHistoryResponse>> history(@PathVariable UUID id) {
        return ApiResponse.ok(claimService.getHistory(id));
    }

    @GetMapping("/{id}/settlement")
    @Operation(summary = "Get the settlement record for a claim")
    public ApiResponse<SettlementResponse> settlement(@PathVariable UUID id) {
        return ApiResponse.ok(claimService.getSettlement(id));
    }

    @PostMapping("/{id}/documents")
    @Operation(summary = "Upload a document for a claim")
    public ApiResponse<DocumentResponse> uploadDocument(@CurrentUser UUID userId,
                                                        @PathVariable UUID id,
                                                        @RequestParam("file") MultipartFile file) {
        User user = loadUser(userId);
        return ApiResponse.ok(DocumentResponse.from(
                documentService.upload(claimService.getClaim(id), file, user)));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List documents of a claim")
    public ApiResponse<List<DocumentResponse>> listDocuments(@PathVariable UUID id) {
        return ApiResponse.ok(documentService.list(claimService.getClaim(id)).stream()
                .map(DocumentResponse::from)
                .toList());
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @Operation(summary = "Download a claim document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
        var document = documentService.get(id, documentId);
        try {
            var path = java.nio.file.Path.of(document.getStoragePath());
            if (!Files.exists(path)) {
                throw new ResourceNotFoundException("Document file not found on disk");
            }
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Document file not found on disk");
        }
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "List notes on a claim")
    public ApiResponse<List<NoteResponse>> notes(@PathVariable UUID id) {
        return ApiResponse.ok(claimService.getNotes(id));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add a note to a claim")
    public ApiResponse<NoteResponse> addNote(@CurrentUser UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody NoteRequest request) {
        return ApiResponse.ok(claimService.addNote(id, request, loadUser(userId)));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a claim to an officer (admin)")
    public ApiResponse<ClaimResponse> assign(@CurrentUser UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody ClaimAssignRequest request) {
        return ApiResponse.ok(claimService.assign(id, request, loadUser(userId)));
    }

    @PostMapping("/{id}/request-info")
    @Operation(summary = "Request additional information (officer)")
    public ApiResponse<ClaimResponse> requestInfo(@CurrentUser UUID userId, @PathVariable UUID id,
                                                  @Valid @RequestBody ClaimInfoRequest request) {
        return ApiResponse.ok(claimService.requestAdditionalInfo(id, request, loadUser(userId)));
    }

    @PostMapping("/{id}/respond-info")
    @Operation(summary = "Respond to an information request with a note (customer)")
    public ApiResponse<ClaimResponse> respondInfo(@CurrentUser UUID userId, @PathVariable UUID id,
                                                  @Valid @RequestBody ClaimInfoRequest request) {
        return ApiResponse.ok(claimService.respondToInfoRequest(userId, id, request));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a claim (officer)")
    public ApiResponse<ClaimResponse> approve(@CurrentUser UUID userId, @PathVariable UUID id,
                                              @Valid @RequestBody ClaimDecisionRequest request) {
        return ApiResponse.ok(claimService.approve(id, request, loadUser(userId)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a claim (officer)")
    public ApiResponse<ClaimResponse> reject(@CurrentUser UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody ClaimRejectRequest request) {
        return ApiResponse.ok(claimService.reject(id, request, loadUser(userId)));
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Settle an approved claim (officer)")
    public ApiResponse<ClaimResponse> settle(@CurrentUser UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody SettlementRequest request) {
        return ApiResponse.ok(claimService.settle(id, request, loadUser(userId)));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}