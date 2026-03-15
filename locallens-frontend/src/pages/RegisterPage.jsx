import { useState } from 'react';
import { Container, Card, Form, Button, Alert } from 'react-bootstrap';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

export default function RegisterPage() {
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '', role: 'TRAVELER' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const register = useAuthStore((s) => s.register);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(form.email, form.password, form.firstName, form.lastName, form.role);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally { setLoading(false); }
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <Container className="py-5" style={{ maxWidth: 480 }}>
      <Card className="p-4">
        <h3 className="text-center fw-bold mb-4" style={{ color: 'var(--text-primary)' }}>Create Account</h3>
        {error && <Alert variant="danger">{error}</Alert>}
        <Form onSubmit={handleSubmit}>
          <div className="d-flex gap-2 mb-3">
            <Form.Group className="flex-fill"><Form.Label>First Name</Form.Label><Form.Control value={form.firstName} onChange={update('firstName')} required /></Form.Group>
            <Form.Group className="flex-fill"><Form.Label>Last Name</Form.Label><Form.Control value={form.lastName} onChange={update('lastName')} required /></Form.Group>
          </div>
          <Form.Group className="mb-3"><Form.Label>Email</Form.Label><Form.Control type="email" value={form.email} onChange={update('email')} required /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Password</Form.Label><Form.Control type="password" value={form.password} onChange={update('password')} minLength={8} required /></Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>I am a...</Form.Label>
            <Form.Select value={form.role} onChange={update('role')}>
              <option value="TRAVELER">Traveler</option>
              <option value="CREATOR">Local Creator / Guide</option>
            </Form.Select>
          </Form.Group>
          <Button type="submit" className="btn-accent w-100" disabled={loading}>
            {loading ? 'Creating account...' : 'Create Account'}
          </Button>
        </Form>
        <p className="text-center mt-3" style={{ color: 'var(--text-secondary)' }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </Card>
    </Container>
  );
}
