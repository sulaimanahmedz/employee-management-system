package com.ems.document.service;

import com.ems.document.dto.DocumentRequest;
import com.ems.document.dto.DocumentResponse;
import com.ems.document.entity.EmployeeDocument;
import com.ems.document.repository.EmployeeDocumentRepository;
import com.ems.employee.entity.Employee;
import com.ems.employee.repository.EmployeeRepository;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmployeeDocumentService {
    private final EmployeeRepository employees;
    private final EmployeeDocumentRepository documents;
    public EmployeeDocumentService(EmployeeRepository employees, EmployeeDocumentRepository documents) {
        this.employees = employees;
        this.documents = documents;
    }
    private Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }
    public List<DocumentResponse> list(Long employeeId) {
        employee(employeeId);
        return documents.findByEmployeeIdOrderByIdDesc(employeeId).stream().map(DocumentResponse::from).toList();
    }
    public DocumentResponse.Page page(Long employeeId, int page, java.time.LocalDate expiresBy, EmployeeDocument.Category category, String query) {
        employee(employeeId);
        if (page < 0 || (long) page * 10 > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document page");
        if (query.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document search must be at most 150 characters");
        }
        String term = query.strip().toLowerCase(Locale.ROOT);
        var pageable = org.springframework.data.domain.PageRequest.of(
            page, 10, org.springframework.data.domain.Sort.by("id").descending());
        org.springframework.data.jpa.domain.Specification<EmployeeDocument> filter =
            (root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId);
        if (expiresBy != null) filter = filter.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiresOn"), expiresBy));
        if (category != null) filter = filter.and((root, query, cb) -> cb.equal(root.get("category"), category));
        if (!term.isEmpty()) {
            filter = filter.and((root, criteria, cb) ->
                cb.greaterThan(cb.locate(cb.lower(root.get("name")), term), 0));
        }
        var result = documents.findAll(filter, pageable);
        return new DocumentResponse.Page(result.getContent().stream().map(DocumentResponse::from).toList(),
            page, result.getTotalPages(), result.getTotalElements());
    }
    @Transactional
    public DocumentResponse create(Long employeeId, DocumentRequest request) {
        var saved = documents.saveAndFlush(new EmployeeDocument(employee(employeeId), request.name(), request.category(), request.expiresOn()));
        return DocumentResponse.from(saved);
    }
    public DocumentResponse get(Long employeeId, Long documentId) {
        employee(employeeId);
        return DocumentResponse.from(find(employeeId, documentId));
    }
    @Transactional
    public DocumentResponse update(Long employeeId, Long documentId, DocumentRequest request) {
        employee(employeeId);
        var document = find(employeeId, documentId);
        document.update(request.name(), request.category(), request.expiresOn());
        return DocumentResponse.from(documents.saveAndFlush(document));
    }
    @Transactional
    public void delete(Long employeeId, Long documentId) {
        employee(employeeId);
        documents.delete(find(employeeId, documentId));
    }
    private EmployeeDocument find(Long employeeId, Long documentId) {
        return documents.findByIdAndEmployeeId(documentId, employeeId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Document metadata not found"));
    }
}
