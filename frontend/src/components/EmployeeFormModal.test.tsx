import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { EmployeeFormModal } from './EmployeeFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeAlice } from '../mocks/employeeFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const EMPLOYEES_URL = `${MSW_BASE}/employees`;

function conflict(field: 'login' | 'email') {
  return HttpResponse.json(
    {
      error: 'conflict',
      message: 'Duplicate',
      fields: { [field]: 'already exists' },
      timestamp: new Date().toISOString(),
    },
    { status: 409 },
  );
}

async function fillValidCreateForm(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Carol');
  await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Carter');
  await user.type(screen.getByLabelText(/^usuario$|^username$/i), 'ccarter');
  await user.type(screen.getByLabelText(/^email$/i), 'carol@aleatica.com');
}

describe('EmployeeFormModal', () => {
  it('should_create_employee_when_form_is_valid', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(EMPLOYEES_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...employeeAlice, id: 50 }, { status: 201 });
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={onSaved} />);

    await fillValidCreateForm(user);
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(sentBody).toMatchObject({ login: 'ccarter', email: 'carol@aleatica.com' });
  });

  it('should_show_login_error_when_create_returns_409_duplicate_login', async () => {
    server.use(http.post(EMPLOYEES_URL, () => conflict('login')));
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await fillValidCreateForm(user);
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/ya existe un empleado con ese usuario|already exists.*username/i),
    ).toBeInTheDocument();
  });

  it('should_show_email_error_when_create_returns_409_duplicate_email', async () => {
    server.use(http.post(EMPLOYEES_URL, () => conflict('email')));
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await fillValidCreateForm(user);
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/ya existe un empleado con ese email|already exists.*email/i),
    ).toBeInTheDocument();
  });

  it('should_show_required_errors_when_submitting_empty', async () => {
    let called = false;
    server.use(
      http.post(EMPLOYEES_URL, () => {
        called = true;
        return HttpResponse.json(employeeAlice, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findAllByText(/este campo es obligatorio|this field is required/i),
    ).not.toHaveLength(0);
    expect(called).toBe(false);
  });

  it('should_update_employee_when_editing_valid_data', async () => {
    let putId: string | undefined;
    let putBody: Record<string, unknown> | null = null;
    server.use(
      http.put(`${EMPLOYEES_URL}/:id`, async ({ request, params }) => {
        putId = params.id as string;
        putBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...employeeAlice, ...putBody });
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <EmployeeFormModal employee={employeeAlice} onClose={vi.fn()} onSaved={onSaved} />,
    );

    const firstName = screen.getByLabelText(/^nombre$|^first name$/i);
    await user.clear(firstName);
    await user.type(firstName, 'Alicia');
    // El campo login no se edita: ausente en modo edicion.
    expect(screen.queryByLabelText(/^usuario$|^username$/i)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(putId).toBe(String(employeeAlice.id));
    expect(putBody).toMatchObject({ firstName: 'Alicia' });
  });
});
