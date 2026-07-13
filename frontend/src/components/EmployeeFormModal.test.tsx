import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { EmployeeFormModal } from './EmployeeFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeAlice } from '../mocks/employeeFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const EMPLOYEES_URL = `${MSW_BASE}/employees`;
const LOGIN_RE = /usuario \/ login|username \/ login/i;

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
  await user.type(screen.getByLabelText(LOGIN_RE), 'ccarter');
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

  it('should_send_selected_category_in_create_body', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(EMPLOYEES_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...employeeAlice, id: 51 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await fillValidCreateForm(user);
    const categorySelect = screen.getByLabelText(/^categoría$|^category$/i);
    expect(categorySelect).toBeInTheDocument();
    await user.selectOptions(categorySelect, 'DIRECTOR_N1');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(sentBody).toMatchObject({ category: 'DIRECTOR_N1' });
    });
  });

  it('should_default_category_to_empleado_in_create_body', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(EMPLOYEES_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...employeeAlice, id: 52 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await fillValidCreateForm(user);
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(sentBody).toMatchObject({ category: 'EMPLEADO' });
    });
  });

  it('should_prefill_and_send_category_when_editing', async () => {
    let putBody: Record<string, unknown> | null = null;
    server.use(
      http.put(`${EMPLOYEES_URL}/:id`, async ({ request }) => {
        putBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...employeeAlice, ...putBody });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <EmployeeFormModal employee={employeeAlice} onClose={vi.fn()} onSaved={vi.fn()} />,
    );

    const categorySelect = screen.getByLabelText(/^categoría$|^category$/i) as HTMLSelectElement;
    expect(categorySelect.value).toBe(employeeAlice.category);
    await user.selectOptions(categorySelect, 'GERENTE');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(putBody).toMatchObject({ category: 'GERENTE' });
    });
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
    // El campo login no se edita: se muestra de solo lectura, sin input asociado.
    expect(screen.queryByLabelText(LOGIN_RE)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(putId).toBe(String(employeeAlice.id));
    expect(putBody).toMatchObject({ firstName: 'Alicia' });
  });

  it('should_render_tabs_for_details_parking_and_desk', async () => {
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    expect(screen.getByRole('tab', { name: /detalles|details/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /plaza fija|fixed space/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /puesto fijo|fixed desk/i })).toBeInTheDocument();
  });

  it('should_save_fixed_parking_with_resource_type_parking_when_set_in_tab', async () => {
    let assignmentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(EMPLOYEES_URL, () => HttpResponse.json({ ...employeeAlice, id: 77 }, { status: 201 })),
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, async ({ request }) => {
        assignmentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json([]);
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<EmployeeFormModal onClose={vi.fn()} onSaved={onSaved} />);

    await fillValidCreateForm(user);
    await user.click(screen.getByRole('tab', { name: /plaza fija|fixed space/i }));
    await user.selectOptions(screen.getByLabelText(/^plaza$|^space$/i), '1');
    await user.click(screen.getByRole('button', { name: /martes|tuesday/i }));
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(assignmentBody).toMatchObject({
      parkingSpaceId: 1,
      daysOfWeek: [2],
      resourceType: 'PARKING',
    });
  });

  it('should_open_reset_password_modal_from_details_when_editing', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <EmployeeFormModal employee={employeeAlice} onClose={vi.fn()} onSaved={vi.fn()} />,
    );

    await user.click(screen.getByRole('button', { name: /resetear contraseña|reset password/i }));

    const dialogs = await screen.findAllByRole('dialog');
    expect(
      within(dialogs[dialogs.length - 1]).getByRole('button', {
        name: /generar contraseña temporal|generate temporary/i,
      }),
    ).toBeInTheDocument();
  });
});
