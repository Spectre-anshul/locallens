import { useState } from 'react';
import { Container, Card, Form, Button, Row, Col, Badge } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { tripService, itineraryService } from '../services/dataServices';

const INTERESTS = ['Food', 'Adventure', 'Museums', 'Nightlife', 'Nature', 'Shopping', 'Culture', 'Wellness'];
const STYLES = ['LUXURY', 'COMFORT', 'BACKPACKER', 'FAMILY', 'SOLO', 'COUPLE'];

export default function TripCreatePage() {
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    title: '', destinationCity: '', destinationCountry: '', lat: 0, lng: 0,
    startDate: '', endDate: '', groupSize: 2, travelStyle: 'COMFORT',
    interests: [], accessibilityNeeds: [], budgetTotal: 200000, budgetCurrency: 'USD',
  });
  const navigate = useNavigate();
  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const toggleInterest = (interest) => {
    const list = form.interests.includes(interest)
      ? form.interests.filter((i) => i !== interest)
      : [...form.interests, interest];
    setForm({ ...form, interests: list });
  };

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const { data: trip } = await tripService.createTrip(form);
      await itineraryService.generate(trip.id);
      navigate(`/trip/${trip.id}`);
    } catch (err) { console.error(err); } finally { setLoading(false); }
  };

  return (
    <Container className="py-4" style={{ maxWidth: 700 }}>
      <h2 className="fw-bold mb-4" style={{ color: 'var(--text-primary)' }}>Plan Your Trip ✈️</h2>
      <Card className="p-4">
        {step === 1 && (
          <>
            <h5 className="fw-semibold mb-3">Where & When?</h5>
            <Form.Group className="mb-3"><Form.Label>Trip Title</Form.Label><Form.Control placeholder="Kyoto Adventure 2026" value={form.title} onChange={update('title')} /></Form.Group>
            <Row className="mb-3">
              <Col><Form.Group><Form.Label>City</Form.Label><Form.Control value={form.destinationCity} onChange={update('destinationCity')} /></Form.Group></Col>
              <Col><Form.Group><Form.Label>Country</Form.Label><Form.Control value={form.destinationCountry} onChange={update('destinationCountry')} /></Form.Group></Col>
            </Row>
            <Row className="mb-3">
              <Col><Form.Group><Form.Label>Start Date</Form.Label><Form.Control type="date" value={form.startDate} onChange={update('startDate')} /></Form.Group></Col>
              <Col><Form.Group><Form.Label>End Date</Form.Label><Form.Control type="date" value={form.endDate} onChange={update('endDate')} /></Form.Group></Col>
            </Row>
            <Button className="btn-accent" onClick={() => setStep(2)}>Next →</Button>
          </>
        )}
        {step === 2 && (
          <>
            <h5 className="fw-semibold mb-3">Your Preferences</h5>
            <Form.Group className="mb-3">
              <Form.Label>Budget (USD)</Form.Label>
              <Form.Range min={50000} max={1000000} step={10000} value={form.budgetTotal} onChange={update('budgetTotal')} />
              <div style={{ color: 'var(--text-secondary)' }}>${(form.budgetTotal / 100).toFixed(0)}</div>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Travel Style</Form.Label>
              <div className="d-flex flex-wrap gap-2">
                {STYLES.map((s) => (
                  <Badge key={s} pill bg={form.travelStyle === s ? 'primary' : 'secondary'} style={{ cursor: 'pointer', fontSize: '0.85rem' }}
                    onClick={() => setForm({ ...form, travelStyle: s })}>{s}</Badge>
                ))}
              </div>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Interests</Form.Label>
              <div className="d-flex flex-wrap gap-2">
                {INTERESTS.map((i) => (
                  <Badge key={i} pill bg={form.interests.includes(i) ? 'primary' : 'light'} text={form.interests.includes(i) ? 'white' : 'dark'}
                    style={{ cursor: 'pointer', fontSize: '0.85rem' }} onClick={() => toggleInterest(i)}>{i}</Badge>
                ))}
              </div>
            </Form.Group>
            <Form.Group className="mb-3"><Form.Label>Group Size</Form.Label><Form.Control type="number" min={1} max={20} value={form.groupSize} onChange={update('groupSize')} /></Form.Group>
            <div className="d-flex gap-2">
              <Button variant="outline-secondary" onClick={() => setStep(1)}>← Back</Button>
              <Button className="btn-accent" onClick={handleSubmit} disabled={loading}>
                {loading ? '🤖 Generating with AI...' : 'Create & Generate Itinerary'}
              </Button>
            </div>
          </>
        )}
      </Card>
    </Container>
  );
}
