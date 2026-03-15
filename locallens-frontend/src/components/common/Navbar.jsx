import { Container, Navbar as BSNavbar, Nav, Badge } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useNotificationStore } from '../../store/appStores';
import { FaBell, FaMoon, FaSun, FaMapMarkedAlt, FaCompass, FaPlus } from 'react-icons/fa';

export default function Navbar() {
  const { isAuthenticated, user, logout, darkMode, toggleDarkMode } = useAuthStore();
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const navigate = useNavigate();

  return (
    <BSNavbar expand="lg" className="px-3" style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)' }}>
      <Container fluid>
        <BSNavbar.Brand as={Link} to="/" style={{ color: 'var(--accent)', fontWeight: 700, fontSize: '1.3rem' }}>
          🌍 LocalLens
        </BSNavbar.Brand>
        <BSNavbar.Toggle />
        <BSNavbar.Collapse>
          <Nav className="me-auto">
            {isAuthenticated && (
              <>
                <Nav.Link as={Link} to="/trip/new" style={{ color: 'var(--text-primary)' }}>
                  <FaPlus className="me-1" /> New Trip
                </Nav.Link>
                <Nav.Link as={Link} to="/explore" style={{ color: 'var(--text-primary)' }}>
                  <FaCompass className="me-1" /> Explore
                </Nav.Link>
              </>
            )}
            <Nav.Link as={Link} to="/map" style={{ color: 'var(--text-primary)' }}>
              <FaMapMarkedAlt className="me-1" /> Map
            </Nav.Link>
          </Nav>
          <Nav>
            <Nav.Link onClick={toggleDarkMode} style={{ color: 'var(--text-primary)' }}>
              {darkMode ? <FaSun /> : <FaMoon />}
            </Nav.Link>
            {isAuthenticated ? (
              <>
                <Nav.Link style={{ color: 'var(--text-primary)', position: 'relative' }}>
                  <FaBell />
                  {unreadCount > 0 && (
                    <Badge bg="danger" pill style={{ position: 'absolute', top: 2, right: 2, fontSize: '0.6rem' }}>
                      {unreadCount}
                    </Badge>
                  )}
                </Nav.Link>
                <Nav.Link onClick={() => { logout(); navigate('/'); }} style={{ color: 'var(--text-secondary)' }}>
                  Logout
                </Nav.Link>
              </>
            ) : (
              <>
                <Nav.Link as={Link} to="/login" style={{ color: 'var(--text-primary)' }}>Login</Nav.Link>
                <Nav.Link as={Link} to="/register" className="btn-accent ms-2" style={{ color: '#fff' }}>Sign Up</Nav.Link>
              </>
            )}
          </Nav>
        </BSNavbar.Collapse>
      </Container>
    </BSNavbar>
  );
}
