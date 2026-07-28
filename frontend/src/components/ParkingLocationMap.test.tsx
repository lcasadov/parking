import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders';
import type { ComponentType } from 'react';

// mapbox-gl no funciona en jsdom (sin WebGL): se mockea con instancias falsas que
// registran las llamadas para poder afirmar el comportamiento e invocar los callbacks
// (dragend / click) que el componente engancha al mapa y al marcador.
const mapInstance = {
  addControl: vi.fn(),
  on: vi.fn(),
  setStyle: vi.fn(),
  easeTo: vi.fn(),
  getZoom: vi.fn(() => 15),
  remove: vi.fn(),
};
const markerInstance = {
  setLngLat: vi.fn().mockReturnThis(),
  addTo: vi.fn().mockReturnThis(),
  on: vi.fn(),
  getLngLat: vi.fn(() => ({ lat: 40.5, lng: -3.6 })),
};
const MapMock = vi.fn(() => mapInstance);
const MarkerMock = vi.fn(() => markerInstance);

vi.mock('mapbox-gl', () => ({
  default: {
    accessToken: '',
    Map: MapMock,
    Marker: MarkerMock,
    NavigationControl: vi.fn(),
  },
}));

// El token se lee al evaluar el módulo (const MAPBOX_TOKEN = import.meta.env...): para
// alternar entre "con token" y "sin token" hay que resetear módulos, fijar el env y
// reimportar el componente.
async function loadMap(token: string): Promise<ComponentType<MapProps>> {
  vi.resetModules();
  vi.stubEnv('VITE_MAPBOX_TOKEN', token);
  const mod = await import('./ParkingLocationMap');
  return mod.ParkingLocationMap as ComponentType<MapProps>;
}

interface MapProps {
  lat: number | null;
  lng: number | null;
  address: string;
  onAddressChange: (value: string) => void;
  onChange: (lat: number, lng: number) => void;
}

// Invoca el callback que el componente registró en el mapa/marcador para un evento.
function handlerFor(spy: typeof mapInstance.on, event: string): (arg?: unknown) => void {
  const call = spy.mock.calls.find(([name]) => name === event);
  if (!call) {
    throw new Error(`No handler registered for ${event}`);
  }
  return call[1] as (arg?: unknown) => void;
}

describe('ParkingLocationMap', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        json: async () => ({ features: [{ center: [-3.7038, 40.4168] }] }),
      })),
    );
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

  it('should_show_only_address_input_and_notice_when_token_is_missing', async () => {
    const Map = await loadMap('');
    const onAddressChange = vi.fn();
    renderWithProviders(
      <Map lat={null} lng={null} address="" onAddressChange={onAddressChange} onChange={vi.fn()} />,
    );

    // Sin token: se muestra el campo de dirección (para no bloquear el modo solo-texto)
    // y un aviso, pero NO se inicializa el mapa.
    await userEvent.type(screen.getByLabelText(/dirección|address/i), 'Calle X');
    expect(onAddressChange).toHaveBeenCalled();
    expect(screen.getByText(/token de mapbox|mapbox token/i)).toBeInTheDocument();
    expect(MapMock).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: /buscar|find on map/i })).not.toBeInTheDocument();
  });

  it('should_initialize_map_and_show_hint_when_token_present_without_coords', async () => {
    const Map = await loadMap('pk.test');
    renderWithProviders(
      <Map lat={null} lng={null} address="" onAddressChange={vi.fn()} onChange={vi.fn()} />,
    );

    await waitFor(() => expect(MapMock).toHaveBeenCalled());
    expect(MarkerMock).toHaveBeenCalled();
    expect(screen.getByText(/arrastra el marcador|drag the marker/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /buscar|find on map/i })).toBeInTheDocument();
  });

  it('should_show_pinned_coords_when_lat_lng_present', async () => {
    const Map = await loadMap('pk.test');
    renderWithProviders(
      <Map lat={40.5405} lng={-3.651} address="Av" onAddressChange={vi.fn()} onChange={vi.fn()} />,
    );

    await waitFor(() => expect(MapMock).toHaveBeenCalled());
    expect(screen.getByText(/40\.540500|punto fijado|pinned point/i)).toBeInTheDocument();
  });

  it('should_geocode_address_and_report_coords_when_search_clicked', async () => {
    const Map = await loadMap('pk.test');
    const onChange = vi.fn();
    renderWithProviders(
      <Map lat={null} lng={null} address="Av. de Europa 18" onAddressChange={vi.fn()} onChange={onChange} />,
    );
    await waitFor(() => expect(MapMock).toHaveBeenCalled());

    await userEvent.click(screen.getByRole('button', { name: /buscar|find on map/i }));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(40.4168, -3.7038));
    expect(fetch).toHaveBeenCalled();
  });

  it('should_show_error_when_geocoding_finds_nothing', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({ json: async () => ({ features: [] }) })),
    );
    const Map = await loadMap('pk.test');
    renderWithProviders(
      <Map lat={null} lng={null} address="Nowhere" onAddressChange={vi.fn()} onChange={vi.fn()} />,
    );
    await waitFor(() => expect(MapMock).toHaveBeenCalled());

    await userEvent.click(screen.getByRole('button', { name: /buscar|find on map/i }));

    expect(
      await screen.findByText(/no se encontró la dirección|address not found/i),
    ).toBeInTheDocument();
  });

  it('should_switch_map_style_when_satellite_toggle_clicked', async () => {
    const Map = await loadMap('pk.test');
    renderWithProviders(
      <Map lat={null} lng={null} address="" onAddressChange={vi.fn()} onChange={vi.fn()} />,
    );
    await waitFor(() => expect(MapMock).toHaveBeenCalled());

    await userEvent.click(screen.getByRole('button', { name: /satélite|satellite/i }));

    expect(mapInstance.setStyle).toHaveBeenCalled();
  });

  it('should_report_coords_when_marker_dragged_or_map_clicked', async () => {
    const Map = await loadMap('pk.test');
    const onChange = vi.fn();
    renderWithProviders(
      <Map lat={null} lng={null} address="" onAddressChange={vi.fn()} onChange={onChange} />,
    );
    await waitFor(() => expect(MapMock).toHaveBeenCalled());

    // Arrastrar el marcador (getLngLat → 40.5, -3.6).
    handlerFor(markerInstance.on, 'dragend')();
    expect(onChange).toHaveBeenCalledWith(40.5, -3.6);

    // Clic en el mapa recoloca el pin en el punto pulsado.
    handlerFor(mapInstance.on, 'click')({ lngLat: { lat: 41, lng: -3 } });
    expect(markerInstance.setLngLat).toHaveBeenCalledWith({ lat: 41, lng: -3 });
    expect(onChange).toHaveBeenCalledWith(41, -3);
  });
});
