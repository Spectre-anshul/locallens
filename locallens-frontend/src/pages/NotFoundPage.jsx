import { Container } from 'react-bootstrap';
import { Link } from 'react-router-dom';
export default function NotFoundPage() {
  return (
    <Container className="py-5 text-center">
      <h1 className="display-1 fw-bold" style={{ color: 'var(--accent)' }}>404</h1>
      <p style={{ color: 'var(--text-secondary)' }}>This page doesn't exist.</p>
      <Link to="/" className="btn btn-accent">Go Home</Link>
    </Container>
  );
}
