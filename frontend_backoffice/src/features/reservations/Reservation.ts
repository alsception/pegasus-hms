export interface Reservation {
  id: number;
  bookerId: number | null;
  roomId: number | null;
  roomNumber: number | string | null;
  checkIn: string;
  checkOut: string;
  expectedArrivalTime: string | null;
  expectedDepartureTime: string | null;
  guests: number;
  status: ReservationStatus;
  totalPrice: number | null;
  notes: string | null;
  created?: string;
  modified?: string;
}

type ReservationStatus =
    | "PENDING"
    | "CONFIRMED"
    | "CHECKED_IN"
    | "CHECKED_OUT"
    | "CANCELLED"
    | "NO_SHOW";