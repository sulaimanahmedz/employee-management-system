package com.ems.employee;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/documents")
public class EmployeeDocumentController {
    private final EmployeeDocumentService service;
    public EmployeeDocumentController(EmployeeDocumentService service) { this.service = service; }
    @GetMapping
    public List<DocumentResponse> list(@PathVariable Long employeeId) { return service.list(employeeId); }
    @PostMapping
    public ResponseEntity<DocumentResponse> create(@PathVariable Long employeeId, @Valid @RequestBody DocumentRequest request) {
        var saved = service.create(employeeId, request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employeeId + "/documents/" + saved.id())).body(saved);
    }
    @GetMapping("/{documentId}")
    public DocumentResponse get(@PathVariable Long employeeId, @PathVariable Long documentId) {
        return service.get(employeeId, documentId);
    }
    @PutMapping("/{documentId}")
    public DocumentResponse update(@PathVariable Long employeeId, @PathVariable Long documentId,
            @Valid @RequestBody DocumentRequest request) { return service.update(employeeId, documentId, request); }
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long employeeId, @PathVariable Long documentId) { service.delete(employeeId, documentId); }
}
