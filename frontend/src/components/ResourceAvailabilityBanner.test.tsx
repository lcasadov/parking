import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ResourceAvailabilityBanner } from './ResourceAvailabilityBanner';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/requests';

const AVAILABILITY_URL = `${MSW_BASE}/availability`;

describe('ResourceAvailabilityBanner', () => {
  it('should_show_the_available_count_for_the_resource', async () => {
    server.use(
      http.get(AVAILABILITY_URL, ({ request }) => {
        const date = new URL(request.url).searchParams.get('date');
        return HttpResponse.json({
          date,
          availableResources: [
            { parkingSpaceId: 1, label: 'P-01' },
            { parkingSpaceId: 2, label: 'P-02' },
          ],
        });
      }),
    );
    renderWithProviders(<ResourceAvailabilityBanner date={todayIso()} resourceType="DESK" />);

    expect(await screen.findByText(/2 disponible|2 available/i)).toBeInTheDocument();
  });

  it('should_pass_the_resource_type_as_query_param', async () => {
    let seenType: string | null = null;
    server.use(
      http.get(AVAILABILITY_URL, ({ request }) => {
        seenType = new URL(request.url).searchParams.get('resourceType');
        return HttpResponse.json({ date: todayIso(), availableResources: [] });
      }),
    );
    renderWithProviders(<ResourceAvailabilityBanner date={todayIso()} resourceType="DESK" />);

    // Sin disponibilidad (0), el banner ya no muestra "0 disponible(s)": comunica
    // con honestidad que suele liberarse (capability request-waitlist).
    expect(
      await screen.findByText(/no quedan puestos libres este día|there are no desks left today/i),
    ).toBeInTheDocument();
    expect(seenType).toBe('DESK');
  });

  it('should_show_the_honest_waitlist_hint_without_a_cta_when_resource_is_not_selected', async () => {
    server.use(
      http.get(AVAILABILITY_URL, ({ request }) =>
        HttpResponse.json({
          date: new URL(request.url).searchParams.get('date'),
          availableResources: [],
        }),
      ),
    );
    renderWithProviders(<ResourceAvailabilityBanner date={todayIso()} resourceType="PARKING" />);

    expect(
      await screen.findByText(/no quedan plazas libres este día|there are no spaces left today/i),
    ).toBeInTheDocument();
    // Sin `selected`/`onJoinWaitlist` no se ofrece el CTA de apuntarse.
    expect(
      screen.queryByRole('button', { name: /apuntarme a la lista de espera|join the waitlist/i }),
    ).not.toBeInTheDocument();
  });

  it('should_render_nothing_when_date_is_in_the_past', () => {
    const { container } = renderWithProviders(
      <ResourceAvailabilityBanner date={addDaysIso(todayIso(), -1)} resourceType="PARKING" />,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
