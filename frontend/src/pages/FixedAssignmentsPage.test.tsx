import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { FixedAssignmentsPage } from './FixedAssignmentsPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { pageOfFixedAssignments, aliceMonday } from '../mocks/fixedAssignmentFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const CONFLICT_BODY = {
  error: 'conflict',
  message: 'space taken',
  timestamp: '2026-03-01T09:00:00Z',
};
const BAD_REQUEST_BODY = {
  error: 'validation',
  message: 'invalid days',
  timestamp: '2026-03-01T09:00:00Z',
};

async function openCreateForm(): Promise<void> {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /nueva asignación|new assignment/i }));
  await screen.findByRole('dialog');
}

async function fillValidForm(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
  await user.selectOptions(dialog.getByLabelText(/plaza|space/i), '1');
  await user.click(dialog.getByRole('checkbox', { name: /lunes|monday/i }));
  await user.click(dialog.getByRole('button', { name: /guardar|save/i }));
}

describe('FixedAssignmentsPage (ADMIN)', () => {
  it('should_list_active_assignments_grouped_when_admin_opens_view', async () => {
    renderWithProviders(<FixedAssignmentsPage />);

    expect(await screen.findByText('Alice Andersson')).toBeInTheDocument();
    expect(screen.getByText('P-01')).toBeInTheDocument();
    const aliceRow = screen.getByText('Alice Andersson').closest('tr');
    expect(within(aliceRow as HTMLElement).getByText(/lunes|monday/i)).toBeInTheDocument();
  });

  it('should_save_and_close_when_admin_submits_valid_assignment', async () => {
    renderWithProviders(<FixedAssignmentsPage />);
    await openCreateForm();
    await fillValidForm();

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('should_show_conflict_toast_when_put_returns_409', async () => {
    server.use(
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
        HttpResponse.json(CONFLICT_BODY, { status: 409 }),
      ),
    );
    renderWithProviders(
      <>
        <FixedAssignmentsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    await fillValidForm();

    expect(
      await screen.findByText(/ya tiene una asignación fija|already has a fixed assignment/i),
    ).toBeInTheDocument();
  });

  it('should_show_invalid_days_toast_when_put_returns_400', async () => {
    server.use(
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
        HttpResponse.json(BAD_REQUEST_BODY, { status: 400 }),
      ),
    );
    renderWithProviders(
      <>
        <FixedAssignmentsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    await fillValidForm();

    expect(
      await screen.findByText(/días inválidos|invalid days/i),
    ).toBeInTheDocument();
  });

  it('should_block_submit_and_show_error_when_no_day_selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FixedAssignmentsPage />);
    await openCreateForm();
    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    await user.selectOptions(dialog.getByLabelText(/plaza|space/i), '1');
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    expect(
      dialog.getByText(/selecciona al menos un día|select at least one day/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should_require_employee_when_submitting_empty_form', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FixedAssignmentsPage />);
    await openCreateForm();
    const dialog = within(screen.getByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    expect(dialog.getByRole('alert')).toHaveTextContent(
      /selecciona un empleado|select an employee/i,
    );
  });

  it('should_require_space_when_only_employee_selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FixedAssignmentsPage />);
    await openCreateForm();
    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    expect(dialog.getByRole('alert')).toHaveTextContent(
      /selecciona una plaza|select a space/i,
    );
  });

  it('should_show_generic_toast_when_put_fails_with_server_error', async () => {
    server.use(
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
        HttpResponse.json({ error: 'server', message: 'boom' }, { status: 500 }),
      ),
    );
    renderWithProviders(
      <>
        <FixedAssignmentsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    await fillValidForm();

    expect(
      await screen.findByText(/no se pudo guardar la asignación fija|could not save the fixed assignment/i),
    ).toBeInTheDocument();
  });

  it('should_revoke_when_admin_confirms_revocation', async () => {
    const user = userEvent.setup();
    let revoked = false;
    server.use(
      http.delete(`${MSW_BASE}/fixed-assignments/employee/:id`, () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<FixedAssignmentsPage />);

    await screen.findByText('Alice Andersson');
    const aliceRow = screen.getByText('Alice Andersson').closest('tr') as HTMLElement;
    await user.click(within(aliceRow).getByRole('button', { name: /revocar|revoke/i }));

    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByText(/alice andersson/i)).toBeInTheDocument();
    await user.click(dialog.getByRole('button', { name: /revocar|revoke/i }));

    await waitFor(() => expect(revoked).toBe(true));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_render_pagination_when_more_than_one_page', async () => {
    server.use(
      http.get(`${MSW_BASE}/fixed-assignments`, () =>
        HttpResponse.json(
          pageOfFixedAssignments([aliceMonday], { totalPages: 2, first: true, last: false }),
        ),
      ),
    );
    renderWithProviders(<FixedAssignmentsPage />);

    const next = await screen.findByRole('button', { name: /siguiente|next/i });
    expect(next).toBeEnabled();
    expect(screen.getByRole('button', { name: /anterior|previous/i })).toBeDisabled();
  });

  it('should_send_resource_type_desk_when_assigning_a_desk', async () => {
    const user = userEvent.setup();
    let putBody: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, async ({ request }) => {
        putBody = await request.json();
        return HttpResponse.json([]);
      }),
    );
    renderWithProviders(<FixedAssignmentsPage />);
    await openCreateForm();

    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    // Elegir recurso "Puesto" cambia la lista a puestos (D-xx).
    await user.selectOptions(dialog.getByLabelText(/tipo de recurso|resource type/i), 'DESK');
    await dialog.findByRole('option', { name: 'D-01' });
    await user.selectOptions(dialog.getByLabelText(/^puesto$|^desk$/i), '1');
    await user.click(dialog.getByRole('checkbox', { name: /lunes|monday/i }));
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() =>
      expect(putBody).toEqual({ parkingSpaceId: 1, daysOfWeek: [1], resourceType: 'DESK' }),
    );
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_show_resource_type_pill_in_assignment_rows', async () => {
    renderWithProviders(<FixedAssignmentsPage />);

    await screen.findByText('Alice Andersson');
    // Las asignaciones sin resourceType son de plaza (retrocompatible).
    expect(screen.getAllByText(/^plaza$|^space$/i).length).toBeGreaterThan(0);
  });
});
