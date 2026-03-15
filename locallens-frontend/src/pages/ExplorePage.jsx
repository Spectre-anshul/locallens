import { useState } from 'react';
import { Container, Row, Col, Card, Form, Badge, Spinner } from 'react-bootstrap';
import { useQuery } from '@tanstack/react-query';
import { experienceService } from '../services/dataServices';
import { FaStar, FaClock, FaUsers } from 'react-icons/fa';

export default function ExplorePage() {
  const [city, setCity] = useState('Kyoto');
  const [category, setCategory] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['experiences', city, category],
    queryFn: () => experienceService.search({ city, category: category || undefined }).then((r) => r.data),
    enabled: !!city,
  });

  const experiences = data?.content || [];

  return (
    <Container className="py-4">
      <h2 className="fw-bold mb-4" style={{ color: 'var(--text-primary)' }}>Explore Experiences 🌟</h2>
      <Row className="mb-4">
        <Col md={4}><Form.Control placeholder="City" value={city} onChange={(e) => setCity(e.target.value)} /></Col>
        <Col md={4}>
          <Form.Select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All Categories</option>
            {['FOOD', 'CULTURE', 'ADVENTURE', 'NIGHTLIFE', 'WELLNESS', 'NATURE'].map((c) => (<option key={c} value={c}>{c}</option>))}
          </Form.Select>
        </Col>
      </Row>
      {isLoading ? <Spinner animation="border" /> : (
        <Row className="g-3">
          {experiences.map((exp) => (
            <Col md={4} key={exp.id}>
              <Card className="h-100 fade-in" style={{ cursor: 'pointer' }}>
                <div style={{ height: 180, background: 'linear-gradient(135deg, var(--accent-light), var(--bg-tertiary))', borderRadius: 'var(--radius-md) var(--radius-md) 0 0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <span style={{ fontSize: '3rem' }}>🌍</span>
                </div>
                <Card.Body>
                  <div className="d-flex justify-content-between mb-1">
                    <Badge bg="secondary">{exp.category}</Badge>
                    {exp.hyperlocal && <Badge className="badge-local">LOCAL</Badge>}
                  </div>
                  <Card.Title className="fw-semibold" style={{ fontSize: '1rem' }}>{exp.title}</Card.Title>
                  <div className="d-flex gap-3" style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                    <span><FaStar color="#f59e0b" /> {exp.rating}</span>
                    <span><FaClock /> {exp.duration}min</span>
                    <span><FaUsers /> max {exp.maxGroupSize}</span>
                  </div>
                  <div className="mt-2 fw-bold" style={{ color: 'var(--accent)' }}>
                    {exp.price?.currency} {(exp.price?.amount / 100).toFixed(0)}
                    <small style={{ fontWeight: 400 }}> /{exp.price?.pricingType === 'PER_GROUP' ? 'group' : 'person'}</small>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
          {experiences.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No experiences found. Try a different city or category.</p>}
        </Row>
      )}
    </Container>
  );
}
