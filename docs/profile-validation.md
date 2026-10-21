# Employee profile acceptance checks

Status: pending execution. These checks accompany the October 21, 2026 profile increment. Run them only during authorized weekly maintenance or when the user explicitly requests execution.

## Backend automated coverage

Run the existing Maven test suite during maintenance. EmployeeApiTest includes:
- All profile fields persist and are returned normalized by GET and reflected in the directory.
- Invalid email updates return 400 and leave the stored email unchanged.
- Updating to another employee's email returns 409 and rolls back all edited fields.
- Retaining the same employee's email, with different capitalization, succeeds.
- Updating a deleted employee returns 404 without creating a replacement record.

## Browser acceptance scenarios

Use synthetic records only, for example Aisha Khan (aisha@example.com) and Noor Ali (noor@example.com).

1. Create both employees. Select Aisha's name using the keyboard. Verify the profile loads from GET and displays all fields and employee ID.
2. Select Edit profile. Change the department, job title and status; save. Verify the profile shows server-returned values and the success message. Return to the directory and check that its row, active count and department count reflect the update. Reload and reopen to check persistence.
3. Change a field and cancel. Reject the discard confirmation and verify the draft remains. Cancel again and accept; verify the original values return. Repeat with Back to directory.
4. Enter an invalid email and verify browser validation prevents submission. For server validation, use a whitespace-only name; verify an error is shown and the draft remains editable.
5. Change Aisha's email to Noor's. Verify the server conflict is displayed, the draft remains, and saved profile values are unchanged. Correct the email and save successfully.
6. Simulate a network failure on PUT. Verify the form becomes editable again and retains all input. Restore connectivity and retry.
7. Delay PUT. Verify fields, Cancel and Back are disabled while saving and a second submit cannot issue a duplicate request.
8. Delay GET and return to the directory before it completes. Open a different employee; verify the earlier response cannot replace the new profile.
9. Delete an employee via the API after loading the directory, then open their profile. Verify the error, Retry and Back actions. Delete a profile while its edit form is open and verify PUT 404 leaves the draft visible.
10. At a narrow viewport, check labels, long email addresses, buttons and the profile details layout. Check focus visibility and that the profile heading receives focus on opening.

## Known scope

No authentication or concurrent-edit version check is introduced. Updates retain the backend's existing last-write-wins behavior. Unsaved-change prompts cover in-app Cancel/Back and browser unload where the browser supports it. These steps are acceptance instructions, not a claim that browser tests have run.
