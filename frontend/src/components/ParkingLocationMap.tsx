import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import mapboxgl from 'mapbox-gl';
import { Button } from './Button';

// Token público de Mapbox (pk.*): es cliente por diseño (restringible por dominio en el
// dashboard de Mapbox). Se lee de las variables VITE_ expuestas por Vite en build.
const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN as string | undefined;

// Centro por defecto cuando aún no hay coordenadas fijadas (Alcobendas / Av. de Europa,
// sede de referencia). Solo es el punto de partida del mapa; el admin arrastra el pin.
const DEFAULT_CENTER: [number, number] = [-3.6415, 40.5411];
const DEFAULT_ZOOM = 15;
const PLACED_ZOOM = 16;

// Estilos Mapbox por tema (la app conmuta con body.theme-dark) y vista satélite.
const STYLE_LIGHT = 'mapbox://styles/mapbox/streets-v12';
const STYLE_DARK = 'mapbox://styles/mapbox/dark-v11';
const STYLE_SATELLITE = 'mapbox://styles/mapbox/satellite-streets-v12';

interface ParkingLocationMapProps {
  // Coordenadas actuales del pin (o null si aún no hay punto fijado).
  lat: number | null;
  lng: number | null;
  // Dirección postal escrita: destino del botón "Buscar en el mapa" (geocodificación).
  address: string;
  // Se invoca al editar el campo de dirección (el input vive en este componente para
  // poder colocar el botón "Buscar" a su derecha).
  onAddressChange: (value: string) => void;
  // Se invoca al arrastrar/soltar el pin, al hacer clic en el mapa o tras geocodificar.
  onChange: (lat: number, lng: number) => void;
}

function isDarkTheme(): boolean {
  return document.body.classList.contains('theme-dark');
}

// Estilo vigente según vista (satélite fija su propio estilo; el mapa normal sigue el tema).
function styleFor(satellite: boolean): string {
  if (satellite) {
    return STYLE_SATELLITE;
  }
  return isDarkTheme() ? STYLE_DARK : STYLE_LIGHT;
}

// Mapa interactivo de Mapbox con un marcador arrastrable para fijar el punto EXACTO del
// parking (más preciso que geocodificar el texto). Colocar pin = arrastrarlo o hacer clic
// en el mapa; el botón "Buscar" geocodifica la dirección escrita y recoloca el pin.
export function ParkingLocationMap({
  lat,
  lng,
  address,
  onAddressChange,
  onChange,
}: ParkingLocationMapProps) {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markerRef = useRef<mapboxgl.Marker | null>(null);
  // onChange puede cambiar de identidad entre renders; lo guardamos en ref para no
  // reinicializar el mapa (efecto de montaje una sola vez).
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState(false);
  const [satellite, setSatellite] = useState(false);
  // Ref para que el observador de tema (closure de montaje) lea la vista actual.
  const satelliteRef = useRef(satellite);
  satelliteRef.current = satellite;

  // Inicializa el mapa una única vez (montaje). Limpia en el desmontaje.
  useEffect(() => {
    if (!MAPBOX_TOKEN || !containerRef.current || mapRef.current) {
      return;
    }
    mapboxgl.accessToken = MAPBOX_TOKEN;
    const hasPoint = lat !== null && lng !== null;
    const center: [number, number] = hasPoint ? [lng as number, lat as number] : DEFAULT_CENTER;
    const map = new mapboxgl.Map({
      container: containerRef.current,
      style: styleFor(satelliteRef.current),
      center,
      zoom: hasPoint ? PLACED_ZOOM : DEFAULT_ZOOM,
      attributionControl: true,
    });
    map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-right');

    const marker = new mapboxgl.Marker({ draggable: true, color: '#0f9e68' })
      .setLngLat(center)
      .addTo(map);
    marker.on('dragend', () => {
      const pos = marker.getLngLat();
      onChangeRef.current(pos.lat, pos.lng);
    });
    // Clic en cualquier punto del mapa = recolocar el pin ahí.
    map.on('click', (event) => {
      marker.setLngLat(event.lngLat);
      onChangeRef.current(event.lngLat.lat, event.lngLat.lng);
    });

    mapRef.current = map;
    markerRef.current = marker;
    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
    // Solo al montar: el resto se refleja vía efectos dedicados abajo.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Refleja cambios EXTERNOS de coordenadas (p. ej. reset del formulario) en el pin.
  useEffect(() => {
    const map = mapRef.current;
    const marker = markerRef.current;
    if (!map || !marker || lat === null || lng === null) {
      return;
    }
    const pos = marker.getLngLat();
    // Evita bucles: solo mueve si difiere del pin actual (el arrastre ya llamó onChange).
    if (Math.abs(pos.lat - lat) > 1e-7 || Math.abs(pos.lng - lng) > 1e-7) {
      marker.setLngLat([lng, lat]);
      map.easeTo({ center: [lng, lat], zoom: Math.max(map.getZoom(), PLACED_ZOOM) });
    }
  }, [lat, lng]);

  // Conmuta el estilo del mapa cuando cambia el tema de la app (observa body.theme-dark).
  useEffect(() => {
    const map = mapRef.current;
    if (!map) {
      return;
    }
    const observer = new MutationObserver(() => {
      // No pisar la vista satélite al cambiar el tema.
      if (!satelliteRef.current) {
        map.setStyle(styleFor(false));
      }
    });
    observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  // Geocodifica la dirección escrita (forward geocoding de Mapbox) y recoloca el pin.
  async function searchAddress(): Promise<void> {
    const query = address.trim();
    if (!MAPBOX_TOKEN || query === '') {
      return;
    }
    setSearching(true);
    setSearchError(false);
    try {
      const url =
        `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json` +
        `?limit=1&language=es&access_token=${MAPBOX_TOKEN}`;
      const response = await fetch(url);
      const body = (await response.json()) as { features?: Array<{ center: [number, number] }> };
      const hit = body.features?.[0]?.center;
      if (!hit) {
        setSearchError(true);
        return;
      }
      const [foundLng, foundLat] = hit;
      markerRef.current?.setLngLat([foundLng, foundLat]);
      mapRef.current?.easeTo({ center: [foundLng, foundLat], zoom: PLACED_ZOOM });
      onChangeRef.current(foundLat, foundLng);
    } catch {
      setSearchError(true);
    } finally {
      setSearching(false);
    }
  }

  const hasToken = Boolean(MAPBOX_TOKEN);

  return (
    <div className="parking-map">
      {/* Fila dirección + "Buscar": el botón queda a la derecha del input y, en móvil,
          salta a otra línea (flex-wrap). El input se muestra siempre, incluso sin token. */}
      <div className="parking-map-search">
        <div className="parking-map-field">
          <label className="field-label" htmlFor="parking-address">
            {t('settings.parkingAddress.label')}
          </label>
          <input
            id="parking-address"
            type="text"
            className="field-input"
            maxLength={500}
            placeholder={t('settings.parkingAddress.placeholder')}
            value={address}
            onChange={(event) => onAddressChange(event.target.value)}
          />
        </div>
        {hasToken ? (
          <Button
            variant="white"
            icon="search"
            loading={searching}
            disabled={address.trim() === ''}
            onClick={() => void searchAddress()}
          >
            {t('settings.parkingAddress.map.search')}
          </Button>
        ) : null}
      </div>

      {hasToken ? (
        <>
          <div className="parking-map-canvas" ref={containerRef} />
          <div className="parking-map-tools">
            <Button
              variant="white"
              icon={satellite ? 'map' : 'satellite'}
              aria-pressed={satellite}
              onClick={() => {
                const next = !satellite;
                setSatellite(next);
                mapRef.current?.setStyle(styleFor(next));
              }}
            >
              {t(
                satellite
                  ? 'settings.parkingAddress.map.streets'
                  : 'settings.parkingAddress.map.satellite',
              )}
            </Button>
            {lat !== null && lng !== null ? (
              <span className="parking-map-coords">
                {t('settings.parkingAddress.map.coords', {
                  lat: lat.toFixed(6),
                  lng: lng.toFixed(6),
                })}
              </span>
            ) : (
              <span className="parking-map-hint">{t('settings.parkingAddress.map.hint')}</span>
            )}
          </div>
          {searchError ? (
            <p className="parking-map-error" role="status">
              {t('settings.parkingAddress.map.searchError')}
            </p>
          ) : null}
        </>
      ) : (
        <p className="hint parking-map-missing" role="status">
          {t('settings.parkingAddress.map.noToken')}
        </p>
      )}
    </div>
  );
}
