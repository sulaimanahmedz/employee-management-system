# Directory acceptance checks

Added October 24, 2026. Execution is pending; run only during authorized maintenance.

## Automated source

EmployeeDirectoryApiTest covers stable page ordering and counts, matching beyond the first page, full-name/email/job-title search, combined department/status search, global aggregates, literal wildcard characters, empty and out-of-range pages, invalid parameters and the legacy list response. Fixtures are synthetic.

## Browser scenarios

1. Create more than 10 synthetic employees across several departments and statuses. Verify 10 rows per page, distinct Previous/Next results, boundary button states and matching counts.
2. Search for an employee outside the initial page. Apply filters and verify they appear. Search by full name, mixed case, email fragment, department and job title.
3. Combine a search with a status filter. Verify only matching rows appear, while overview totals continue to cover the whole organization.
4. Change rows per page and apply. Verify navigation resets to the first page. Clear filters and verify all statuses, empty search and 10 rows are restored.
5. Apply a search with no matches and verify the no-match message, zero results and disabled page navigation.
6. Open a matching profile, change status or department so it no longer matches, save and return. Verify the row disappears and counts refresh; if the last page empties, the directory falls back to a valid page.
7. Create a new employee while filters are active. Verify the current filters remain applied, counts refresh and the new row appears only if it matches.
8. Delay network responses and submit different filters quickly. Verify only the latest response controls the table. Simulate failure, then retry; stale rows must not be presented as the new result.
9. Confirm forms, status selection and page controls work with keyboard navigation and on a narrow screen.
10. Re-run profile acceptance scenarios to check that view/edit navigation and unsaved-change protection still work.

No compilation, tests or browser execution were performed when writing these checks.
