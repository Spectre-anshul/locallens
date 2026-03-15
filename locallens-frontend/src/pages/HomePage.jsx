import { Container, Row, Col, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { FaMapMarkedAlt, FaRobot, FaUsers, FaBolt, FaGlobeAmericas, FaStar } from 'react-icons/fa';

const features = [
  { icon: <FaRobot size={32} />, title: 'AI Itineraries', desc: 'Claude AI creates personalized day-by-day plans with local experiences woven in.' },
  { icon: <FaBolt size={32} />, title: 'Dynamic Replanning', desc: 'Weather? Traffic? Closed venue? Your itinerary auto-adjusts in real-time.' },
  { icon: <FaMapMarkedAlt size={32} />, title: '3D Crowd Maps', desc: 'See where tourists cluster and discover hidden gems with live heatmaps.' },
  { icon: <FaUsers size={32} />, title: 'Local Creators', desc: 'Book authentic experiences from verified local guides — not tourist traps.' },
  { icon: <FaGlobeAmericas size={32} />, title: 'One Dashboard', desc: 'Flights, hotels, budget, events, documents — all in one place.' },
  { icon: <FaStar size={32} />, title: 'Hyperlocal Events', desc: 'Discover tonight\'s underground jazz, secret ramen spots, and neighborhood festivals.' },
];

export default function HomePage() {
  return (
    <div>
      {/* Hero */}
      <section style={{ background: 'linear-gradient(135deg, #4f46e5, #7c3aed, #ec4899)', minHeight: '70vh', display: 'flex', alignItems: 'center' }}>
        <Container className="text-center text-white py-5">
          <h1 className="display-3 fw-bold mb-3 slide-up">Travel Like a Local</h1>
          <p className="lead mb-4 slide-up" style={{ animationDelay: '0.1s', maxWidth: 600, margin: '0 auto' }}>
            AI-powered itineraries. Real-time replanning. Hidden gems from local creators. One dashboard to rule them all.
          </p>
          <div className="slide-up" style={{ animationDelay: '0.2s' }}>
            <Link to="/register"><Button size="lg" variant="light" className="me-3 fw-semibold px-4">Start Planning</Button></Link>
            <Link to="/explore"><Button size="lg" variant="outline-light" className="fw-semibold px-4">Explore Experiences</Button></Link>
          </div>
        </Container>
      </section>

      {/* Features */}
      <section style={{ background: 'var(--bg-secondary)', padding: '5rem 0' }}>
        <Container>
          <h2 className="text-center fw-bold mb-5" style={{ color: 'var(--text-primary)' }}>Why LocalLens?</h2>
          <Row className="g-4">
            {features.map((f, i) => (
              <Col md={4} key={i}>
                <div className="card p-4 h-100 text-center fade-in" style={{ animationDelay: `${i * 0.1}s` }}>
                  <div className="mb-3" style={{ color: 'var(--accent)' }}>{f.icon}</div>
                  <h5 className="fw-semibold" style={{ color: 'var(--text-primary)' }}>{f.title}</h5>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{f.desc}</p>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>
    </div>
  );
}
