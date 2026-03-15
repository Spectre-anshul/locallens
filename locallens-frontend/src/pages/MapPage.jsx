import { Container } from 'react-bootstrap';

export default function MapPage() {
  return (
    <Container fluid className="p-0" style={{ height: 'calc(100vh - 56px)' }}>
      <div style={{ width: '100%', height: '100%', background: 'var(--bg-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div className="text-center">
          <h3 style={{ color: 'var(--text-primary)' }}>🗺️ 3D Crowd Intelligence Map</h3>
          <p style={{ color: 'var(--text-secondary)' }}>
            Deck.gl + MapLibre GL JS map will render here.<br />
            Layers: Heatmap • POI Markers • Hidden Gems • Route • Traffic
          </p>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Install MapLibre GL JS and configure VITE_MAPLIBRE_STYLE in .env
          </p>
        </div>
      </div>
    </Container>
  );
}
