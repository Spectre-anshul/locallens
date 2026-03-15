import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Container, Row, Col, Card, Badge, Nav, Spinner, ProgressBar } from 'react-bootstrap';
import { tripService, itineraryService } from '../services/dataServices';
import { useTripStore } from '../store/appStores';
import { FaPlane, FaHotel, FaCalendarDay, FaWallet, FaMapMarkerAlt, FaClock } from 'react-icons/fa';

export default function DashboardPage() {
  const { tripId } = useParams();
  const { selectedDay, setSelectedDay } = useTripStore();

  const { data: trip, isLoading: tripLoading } = useQuery({
    queryKey: ['trip', tripId], queryFn: () => tripService.getTripById(tripId).then((r) => r.data),
  });

  const { data: days, isLoading: daysLoading } = useQuery({
    queryKey: ['itinerary', tripId], queryFn: () => itineraryService.getDays(tripId).then((r) => r.data),
    refetchInterval: 5000, // poll while generating
  });

  if (tripLoading) return <Container className="py-5 text-center"><Spinner animation="border" /></Container>;

  const currentDay = days?.find((d) => d.dayNumber === selectedDay);
  const budgetPct = trip?.budget ? Math.round((trip.budget.spent / trip.budget.total) * 100) : 0;

  return (
    <Container fluid className="py-4 px-4">
      <h3 className="fw-bold mb-4" style={{ color: 'var(--text-primary)' }}>{trip?.title} 🌍</h3>
      <Row className="g-3">
        {/* Trip Overview */}
        <Col lg={4}>
          <Card className="p-3 mb-3">
            <h6 className="fw-semibold"><FaCalendarDay className="me-2" />{trip?.destination?.city}, {trip?.destination?.country}</h6>
            <small style={{ color: 'var(--text-secondary)' }}>{trip?.startDate} → {trip?.endDate} • {trip?.durationDays} days</small>
            <hr />
            <div className="d-flex justify-content-between align-items-center mb-1">
              <span><FaWallet className="me-1" /> Budget</span>
              <strong>${trip?.budget ? (trip.budget.total / 100).toFixed(0) : 0}</strong>
            </div>
            <ProgressBar now={budgetPct} variant={budgetPct > 80 ? 'danger' : budgetPct > 50 ? 'warning' : 'success'} />
            <small style={{ color: 'var(--text-muted)' }}>{budgetPct}% spent</small>
          </Card>
          <Card className="p-3 mb-3">
            <h6 className="fw-semibold"><FaPlane className="me-2" />Flights</h6>
            {trip?.flights?.length > 0 ? trip.flights.map((f, i) => (
              <div key={i} className="mb-2"><small>{f.airline} {f.flightNumber} • {f.type}</small></div>
            )) : <small style={{ color: 'var(--text-muted)' }}>No flights added yet</small>}
          </Card>
          <Card className="p-3">
            <h6 className="fw-semibold"><FaHotel className="me-2" />Accommodation</h6>
            {trip?.accommodations?.length > 0 ? trip.accommodations.map((a, i) => (
              <div key={i} className="mb-2"><small>{a.name} • {a.type}</small></div>
            )) : <small style={{ color: 'var(--text-muted)' }}>No accommodation added yet</small>}
          </Card>
        </Col>

        {/* Itinerary Timeline */}
        <Col lg={8}>
          <Card className="p-3">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="fw-semibold mb-0">📋 Itinerary</h5>
              {daysLoading && <Spinner animation="border" size="sm" />}
            </div>

            {days && days.length > 0 ? (
              <>
                <Nav variant="pills" className="mb-3 flex-nowrap overflow-auto">
                  {days.map((d) => (
                    <Nav.Item key={d.dayNumber}>
                      <Nav.Link active={selectedDay === d.dayNumber} onClick={() => setSelectedDay(d.dayNumber)}
                        style={{ cursor: 'pointer', whiteSpace: 'nowrap' }}>
                        Day {d.dayNumber}
                      </Nav.Link>
                    </Nav.Item>
                  ))}
                </Nav>

                {currentDay && (
                  <>
                    <p style={{ color: 'var(--accent)', fontWeight: 500 }}>🎯 {currentDay.theme}</p>
                    {currentDay.slots?.map((slot, i) => (
                      <div key={slot.slotId || i} className="card p-3 mb-2 fade-in" style={{ animationDelay: `${i * 0.05}s` }}>
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <div className="d-flex align-items-center gap-2 mb-1">
                              <FaClock size={12} style={{ color: 'var(--text-muted)' }} />
                              <small style={{ color: 'var(--text-secondary)' }}>{slot.startTime} – {slot.endTime}</small>
                              {slot.activity?.hyperlocal && <Badge className="badge-local">LOCAL PICK</Badge>}
                            </div>
                            <strong style={{ color: 'var(--text-primary)' }}>{slot.activity?.title}</strong>
                            {slot.activity?.description && <p className="mb-1 mt-1" style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{slot.activity.description}</p>}
                          </div>
                          {slot.activity?.cost && (
                            <Badge bg="light" text="dark">{slot.activity.cost.currency} {(slot.activity.cost.amount / 100).toFixed(0)}</Badge>
                          )}
                        </div>
                        {slot.transport && (
                          <small style={{ color: 'var(--text-muted)' }}>
                            <FaMapMarkerAlt size={10} /> {slot.transport.mode} • {slot.transport.duration}min
                            {slot.transport.notes && ` • ${slot.transport.notes}`}
                          </small>
                        )}
                      </div>
                    ))}
                  </>
                )}
              </>
            ) : (
              <div className="text-center py-5 pulse">
                <h4>🤖 AI is generating your itinerary...</h4>
                <p style={{ color: 'var(--text-secondary)' }}>This usually takes 10-20 seconds</p>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
