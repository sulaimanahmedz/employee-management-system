package com.ems.employee;

import java.util.List;
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
    public DocumentResponse.Page page(Long employeeId, int page) {
        employee(employeeId);
        if (page < 0 || (long) page * 10 > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document page");
        var result = documents.findByEmployeeId(employeeId, org.springframework.data.domain.PageRequest.of(
            page, 10, org.springframework.data.domain.Sort.by("id").descending()));
        return new DocumentResponse.Page(result.getContent().stream().map(DocumentResponse::from).toList(),
            page, result.getTotalPages(), result.getTotalElements());
    }
    @Transactional
    public DocumentResponse create(Long employeeId, DocumentRequest request) {
        var saved = documents.saveAndFlush(new EmployeeDocument(employee(employeeId), request));
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
        document.update(request);
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
