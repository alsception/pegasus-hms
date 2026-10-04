<script lang="ts">
  import { onMount } from "svelte";
  import api from "../../core/services/client";
  import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
  import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";
  import type { Reservation } from "./Reservation";
  import { showSuccessToast } from "../../core/utils/toaster";
  import { link } from "svelte-spa-router";

  let error: string | null = null;
  let loading = true;
  let saving = false;

  type Room = {
    status: any;
    capacity: any;
    pricePerNight: any;
    id: number;
    roomNumber: string;
    floor: number;
    type: string;
  };

  type ReservationStatus =
    | "PENDING"
    | "CONFIRMED"
    | "CHECKED_IN"
    | "CHECKED_OUT"
    | "CANCELLED"
    | "NO_SHOW";

  // --------------------------------------------------
  // Configuration
  // --------------------------------------------------

  const DAYS_TO_SHOW = 14;
  const ROOM_WIDTH = 110;
  const DAY_WIDTH = 120;
  const ROW_HEIGHT = 64;

  const HOTEL_DAY_START_HOUR = 12;

  const DAY_POSITION_OFFSET = DAY_WIDTH / 2;

  const RESERVATION_HORIZONTAL_PADDING = 4;
  const RESERVATION_TOTAL_HORIZONTAL_PADDING =
    RESERVATION_HORIZONTAL_PADDING * 2;

  const CONTINUES_BEFORE_SYMBOL = "◀";
  const CONTINUES_AFTER_SYMBOL = "▶";

  // Attention: these are 2 different tooltips!

  // Number of first room rows whose tooltip opens downward
  const TOOLTIP_BELOW_ROWS = 5;

  // Extra space reserved for room tooltips
  const ROOM_TOOLTIP_OFFSET = 42    ;

  const DAY_MS = 1000 * 60 * 60 * 24;

  // --------------------------------------------------
  // Data
  // --------------------------------------------------

  let rooms: Room[] = [];
  let reservations: Reservation[] = [];

  // --------------------------------------------------
  // Calendar
  // --------------------------------------------------

  let startDate = new Date();

  startDate.setHours(0, 0, 0, 0);

  let dates: Date[] = [];

  function generateDates() {
    const newDates: Date[] = [];

    for (let i = 0; i < DAYS_TO_SHOW; i++) {
      const date = new Date(startDate);

      date.setDate(startDate.getDate() + i);

      newDates.push(date);
    }

    dates = newDates;
  }

  // --------------------------------------------------
  // Date helpers
  // --------------------------------------------------

  function formatDate(date: Date): string {
    return date.toLocaleDateString("hr-HR", {
      day: "2-digit",
      month: "2-digit",
    });
  }

  function formatWeekday(date: Date): string {
    return date.toLocaleDateString("hr-HR", {
      weekday: "short",
    });
  }

  function dateKey(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");

    return `${y}-${m}-${d}`;
  }

  function addDays(date: Date, days: number): Date {
    const result = new Date(date);

    result.setDate(result.getDate() + days);

    return result;
  }

  function getNights(checkIn: string, checkOut: string): number {
    if (!checkIn || !checkOut) return 0;

    return Math.round(
      (parseDate(checkOut).getTime() - parseDate(checkIn).getTime()) / DAY_MS
    );
  }

  function parseDate(value: string): Date {
    const date = new Date(value);

    date.setHours(0, 0, 0, 0);

    return date;
  }

  // --------------------------------------------------
  // Today
  // --------------------------------------------------

  function isToday(date: Date): boolean {
    const today = new Date();

    today.setHours(0, 0, 0, 0);

    return date.getTime() === today.getTime();
  }

  function isSunday(date: Date): boolean {
    return date.getDay() === 0;
  }

  // --------------------------------------------------
  // Calendar boundaries
  // --------------------------------------------------

  function getCalendarStart(): Date {
    const date = new Date(startDate);

    date.setHours(0, 0, 0, 0);

    return date;
  }

  function getCalendarEnd(): Date {
    const date = new Date(startDate);

    date.setDate(date.getDate() + DAYS_TO_SHOW);

    date.setHours(0, 0, 0, 0);

    return date;
  }

  // --------------------------------------------------
  // Reservation visibility
  // --------------------------------------------------

  function reservationStartsBeforeCalendar(reservation: Reservation): boolean {
    return parseDate(reservation.checkIn) < getCalendarStart();
  }

  function reservationEndsAfterCalendar(reservation: Reservation): boolean {
    return parseDate(reservation.checkOut) > getCalendarEnd();
  }

  // --------------------------------------------------
  // Reservation positioning
  // --------------------------------------------------

  function getReservationStart(reservation: Reservation): number {
    const checkIn = parseDate(reservation.checkIn);
    const calendarStart = getCalendarStart();

    const visibleStart = checkIn < calendarStart ? calendarStart : checkIn;

    const diff = visibleStart.getTime() - calendarStart.getTime();

    return Math.floor(diff / DAY_MS);
  }

  function getReservationEnd(reservation: Reservation): number {
    const checkOut = parseDate(reservation.checkOut);

    const calendarStart = getCalendarStart();
    const calendarEnd = getCalendarEnd();

    const visibleEnd = checkOut > calendarEnd ? calendarEnd : checkOut;

    const diff = visibleEnd.getTime() - calendarStart.getTime();

    return Math.ceil(diff / DAY_MS);
  }

  function getReservationDays(reservation: Reservation): number {
    const start = getReservationStart(reservation);
    const end = getReservationEnd(reservation);

    return Math.max(1, end - start);
  }

  function getReservationStyle(reservation: Reservation): string {
    const start = getReservationStart(reservation);
    const end = getReservationEnd(reservation);

    const startsBeforeCalendar = reservationStartsBeforeCalendar(reservation);

    const endsAfterCalendar = reservationEndsAfterCalendar(reservation);

    const startPosition = startsBeforeCalendar
      ? 0
      : start * DAY_WIDTH + DAY_POSITION_OFFSET;

    const endPosition = endsAfterCalendar
      ? DAYS_TO_SHOW * DAY_WIDTH
      : end * DAY_WIDTH + DAY_POSITION_OFFSET;

    const visibleWidth = endPosition - startPosition;

    return `
      left: ${startPosition + RESERVATION_HORIZONTAL_PADDING}px;
      width: ${Math.max(
        0,
        visibleWidth - RESERVATION_TOTAL_HORIZONTAL_PADDING
      )}px;
    `;
  }

  // --------------------------------------------------
  // Status
  // --------------------------------------------------

  function getStatusClass(status: ReservationStatus): string {
    switch (status) {
      case "CONFIRMED":
        return "bg-accent/60 text-accent-content";

      case "CHECKED_IN":
        return "bg-success/66 text-success-content";

      case "CHECKED_OUT":
        return "bg-neutral/66 text-neutral-content";

      case "PENDING":
        return "bg-warning/66 text-warning-content";

      case "CANCELLED":
        return "bg-error/66 text-error-content";

      default:
        return "bg-base-300";
    }
  }

  function getReservationStatusColor(status: ReservationStatus): string {
    switch (status) {
      case "CONFIRMED":
        return "text-accent";
      case "CHECKED_IN":
        return "text-success";
      case "CHECKED_OUT":
        return "text-base-content/60";
      case "PENDING":
        return "text-warning";
      case "CANCELLED":
        return "text-error";
      default:
        return "text-base-content";
    }
  }

  function getStatusLabel(status: ReservationStatus): string {
    switch (status) {
      case "CONFIRMED":
        return "Potvrđena";

      case "CHECKED_IN":
        return "Prijavljen";

      case "CHECKED_OUT":
        return "Odjavljen";

      case "PENDING":
        return "Na čekanju";

      case "CANCELLED":
        return "Otkazano";

      default:
        return status;
    }
  }

  // --------------------------------------------------
  // Room status
  // --------------------------------------------------

  function getRoomStatusLabel(status: string): string {
    switch (String(status).toUpperCase()) {
      case "AVAILABLE":
        return "SLOBODNA";
      case "OCCUPIED":
        return "ZAUZETA";
      case "CLEANING":
        return "ČIŠĆENJE";
      case "MAINTENANCE":
        return "ODRŽAVANJE";
      case "OUT_OF_SERVICE":
        return "VAN FUNKCIJE";
      default:
        return String(status ?? "").toUpperCase();
    }
  }

  function getRoomStatusColor(status: string): string {
    switch (String(status).toUpperCase()) {
      case "AVAILABLE":
        return "text-success";
      case "OCCUPIED":
        return "text-error";
      case "CLEANING":
        return "text-info";
      case "MAINTENANCE":
      case "OUT_OF_SERVICE":
        return "text-warning";
      default:
        return "text-base-content";
    }
  }

  // --------------------------------------------------
  // Room tooltip (hover + click)
  // --------------------------------------------------

  let openRoomTooltipId: number | null = null;

  function toggleRoomTooltip(roomId: number) {
    openRoomTooltipId = openRoomTooltipId === roomId ? null : roomId;
  }

  // --------------------------------------------------
  // Reservation filtering
  // --------------------------------------------------

  function getRoomReservations(
    roomId: number,
    list: Reservation[]
  ): Reservation[] {
    return list.filter((reservation) => reservation.roomId === roomId);
  }

  function hasReservationConflict(
    reservation: Reservation,
    list: Reservation[]
  ): boolean {
    const checkIn = parseDate(reservation.checkIn);
    const checkOut = parseDate(reservation.checkOut);

    return list.some((other) => {
      if (other.id === reservation.id) return false;
      if (other.roomId !== reservation.roomId) return false;

      const otherCheckIn = parseDate(other.checkIn);
      const otherCheckOut = parseDate(other.checkOut);

      return checkIn < otherCheckOut && checkOut > otherCheckIn;
    });
  }

  // --------------------------------------------------
  // Navigation
  // --------------------------------------------------

  function changePeriod(days: number) {
    const newStartDate = new Date(startDate);

    newStartDate.setDate(newStartDate.getDate() + days);

    newStartDate.setHours(0, 0, 0, 0);

    startDate = newStartDate;

    generateDates();
  }

  function previousPeriod() {
    changePeriod(-1);
  }

  function nextPeriod() {
    changePeriod(1);
  }

  function today() {
    const newStartDate = new Date();

    newStartDate.setHours(0, 0, 0, 0);

    startDate = newStartDate;

    generateDates();
  }

  // --------------------------------------------------
  // Events
  // --------------------------------------------------

  let selectedReservation: Reservation | null = null;

  function handleReservationClick(reservation: Reservation) {
    selectedReservation = reservation;
  }

  function closeModal() {
    selectedReservation = null;
    showNewModal = false;
  }

  function getRoomById(roomId: number): Room | undefined {
    return rooms.find((room) => room.id === roomId);
  }

  // --------------------------------------------------
  // New reservation
  // --------------------------------------------------

  let showNewModal = false;
  let newRoomId: number | null = null;
  let newCheckIn = "";
  let newCheckOut = "";
  let newPricePerNight = 0;
  let newGuestName = "";
  let newGuestSurname = "";
  let newNote = "";

  function handleEmptyCellClick(room: Room, date: Date) {
    newRoomId = room.id;
    newCheckIn = dateKey(date);
    newCheckOut = dateKey(addDays(date, 1));
    newPricePerNight = Number(room.pricePerNight) || 0;
    newGuestName = "";
    newGuestSurname = "";
    newNote = "";
    showNewModal = true;
  }

  function handleNewRoomChange(roomId: number) {
    newRoomId = roomId;

    const room = getRoomById(roomId);

    if (room) {
      newPricePerNight = Number(room.pricePerNight) || 0;
    }
  }

  function hasNewReservationConflict(
    roomId: number,
    checkIn: string,
    checkOut: string
  ): boolean {
    const start = parseDate(checkIn);
    const end = parseDate(checkOut);

    return reservations.some(
      (other) =>
        other.roomId === roomId &&
        other.status !== "CANCELLED" &&
        start < parseDate(other.checkOut) &&
        end > parseDate(other.checkIn)
    );
  }

  async function saveNewReservation() {
    if (newRoomId === null || saving) return;

    const nights = getNights(newCheckIn, newCheckOut);

    const payload = {
      roomId: newRoomId,
      guestName: newGuestName.trim(),
      guestSurname: newGuestSurname.trim(),
      notes: newNote.trim() || null,
      checkIn: newCheckIn,
      checkOut: newCheckOut,
      pricePerNight: newPricePerNight,
      totalPrice: nights * newPricePerNight,
    };

    saving = true;

    try {
      await api<Reservation>("/reservations", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      showSuccessToast("Rezervacija je kreirana");

      showNewModal = false;

      await fetchReservations();
    } catch (err) {
      alert((err as Error).message);
    } finally {
      saving = false;
    }
  }

  // --------------------------------------------------
  // API
  // --------------------------------------------------

  async function fetchRooms() {
    try {
      rooms = await api<Room[]>("/rooms", {
        method: "GET",
      });
    } catch (err) {
      error = (err as Error).message;
    }
  }

  async function fetchReservations() {
    try {
      reservations = await api<Reservation[]>("/reservations", {
        method: "GET",
      });
    } catch (err) {
      error = (err as Error).message;
    }
  }

  // --------------------------------------------------
  // Initial setup
  // --------------------------------------------------

  onMount(async () => {
    try {
      generateDates();

      await Promise.all([fetchRooms(), fetchReservations()]);
    } finally {
      loading = false;

      console.log(reservations);
      console.log(rooms);
    }
  });
</script>

<div class="flex flex-col gap-4">
  <!-- Toolbar -->
  <div class="flex flex-wrap items-center justify-between gap-2">
    <div>
      <h2 class="text-xl font-bold">Rezervacije</h2>

      <p class="text-sm text-base-content/60">Pregled zauzetosti soba</p>
    </div>

    <div class="flex items-center gap-8">
      <button
        class="btn btn-md btn-ghost hover:text-accent"
        onclick={() => (showNewModal = true)}
      >
        <i class="fas fa-plus"></i>
        Nova rezervacija
      </button>

      <div class="join">
        <button class="btn btn-md join-item" onclick={previousPeriod}>
          ‹
        </button>

        <button class="btn btn-md join-item" onclick={today}> Danas </button>

        <button class="btn btn-md join-item" onclick={nextPeriod}> › </button>
      </div>
    </div>
  </div>

  {#if loading}
    <LoadingOverlay />
  {:else if error}
    <ErrorDiv {error} />
  {:else}
    {#key startDate.getTime()}
      <!-- Board -->
      <div
        class="overflow-auto rounded-lg border border-base-300 bg-base-100"
        id="main-calendar-container"
      >
        <div
          class="relative"
          style={`
            width: ${ROOM_WIDTH + dates.length * DAY_WIDTH}px;
            padding-bottom: ${ROOM_TOOLTIP_OFFSET}px;
          `}
        >
          <!-- HEADER -->
          <div
            class="sticky top-0 z-30 flex"
            style={`height: ${ROW_HEIGHT}px;`}
          >
            <!-- Room header -->
            <div
              class="sticky left-0 z-40 flex shrink-0 items-center border-b border-r border-base-300 bg-base-200 px-3 font-bold"
              style={`width: ${ROOM_WIDTH}px;`}
            >
              Soba
            </div>

            <!-- Date headers -->
            {#each dates as date}
              <div
                class={`flex shrink-0 flex-col items-center justify-center border-b border-r border-base-300 ${
                  isSunday(date)
                    ? "bg-error/20 text-primary/80"
                    : "bg-base-200"
                } ${
                  isToday(date)
                    ? "bg-primary/10 text-accent "
                    : "bg-base-200"
                }`}
                style={`width: ${DAY_WIDTH}px;`}
              >
                <span class="text-xs uppercase">
                  {formatWeekday(date)}
                </span>

                <span class="font-bold">
                  {formatDate(date)}
                </span>
              </div>
            {/each}
          </div>

          <!-- ROOMS -->
          {#each rooms as room, roomIndex}
            <div class="relative flex" style={`height: ${ROW_HEIGHT}px;`}>
              <!-- Room -->
              <!-- TOUCH: added role, tabindex, cursor-pointer, onclick, onkeydown -->
              <div
                role="button"
                tabindex="0"
                class="group relative sticky left-0 z-41 flex shrink-0 cursor-pointer flex-col justify-center border-b border-r border-base-300 bg-base-100 px-3 hover:bg-info/10"
                style={`width: ${ROOM_WIDTH}px;`}
                onclick={(e) => {
                  e.stopPropagation();
                  toggleRoomTooltip(room.id);
                }}
                onkeydown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    toggleRoomTooltip(room.id);
                  }
                }}
              >
                <!-- Room number -->
                <span class="text-lg font-bold leading-tight group-hover:text-accent">
                  {room.roomNumber}
                </span>

                <!-- Price + capacity -->
                <div
                  class="mt-1 flex items-center gap-2 text-xs text-base-content/80"
                >
                  <span class="font-medium">
                    € {room.pricePerNight}
                  </span>

                  <span class="flex items-center gap-1">
                    <i class="fas fa-user text-[10px] text-base-content/60"></i>

                    {room.capacity}
                  </span>
                </div>

                <!-- Room tooltip -->
                <!-- TOUCH: class now depends on openRoomTooltipId -->
                <div
                  class={`pointer-events-none absolute left-full z-50 ml-2 w-64 rounded-lg border border-base-300 bg-base-100 p-4 shadow-xl transition-opacity duration-150 ${
                    openRoomTooltipId === room.id
                      ? "opacity-100"
                      : "opacity-0 group-hover:opacity-100"
                  } ${
                    roomIndex < TOOLTIP_BELOW_ROWS
                      ? "top-full -translate-y-1/2"
                      : "bottom-full translate-y-1/2"
                  }`}
                >
                  <div
                    class="mb-3 flex items-center gap-2 border-b border-base-300 pb-2"
                  >
                    <i class="fas fa-door-open text-base-content/60"></i>

                    <span class="font-bold">
                      Soba {room.roomNumber}
                    </span>
                  </div>

                  <div class="space-y-2 text-sm">
                    <div class="flex items-center justify-between gap-4">
                      <span class="text-base-content/60"> Tip sobe </span>

                      <span class="font-medium">
                        {room.type}
                      </span>
                    </div>

                    <div class="flex items-center justify-between gap-4">
                      <span class="text-base-content/60"> Kat </span>

                      <span class="font-medium">
                        {room.floor}
                      </span>
                    </div>

                    <div class="flex items-center justify-between gap-4">
                      <span class="text-base-content/60"> Kapacitet </span>

                      <span class="font-medium">
                        <i class="fas fa-user mr-1 text-xs text-base-content/50"
                        ></i>

                        {room.capacity}
                      </span>
                    </div>

                    <div class="flex items-center justify-between gap-4">
                      <span class="text-base-content/60"> Cijena / noć </span>

                      <span class="font-semibold">
                        € {room.pricePerNight}
                      </span>
                    </div>

                    <div class="flex items-center justify-between gap-4">
                      <span class="text-base-content/60"> Status </span>

                      <span class={`font-bold tracking-wide ${getRoomStatusColor(room.status)}`}>
                        {getRoomStatusLabel(room.status)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Calendar -->
              <div
                class="relative shrink-0"
                style={`width: ${dates.length * DAY_WIDTH}px;`}
              >
                <!-- Grid -->
                <div class="absolute inset-0 flex">
                  {#each dates as date}
                    <button
                      class={`group h-full shrink-0 border-b border-r border-base-300 transition hover:bg-base-200 ${
                        isToday(date) ? "bg-primary/5" : ""
                      }`}
                      style={`width: ${DAY_WIDTH}px;`}
                      aria-label={`Nova rezervacija za sobu ${room.roomNumber} ${formatDate(date)}`}
                      onclick={() => handleEmptyCellClick(room, date)}
                    >
                      <i
                        class="fas fa-plus text-base-content/40 opacity-0 transition-opacity group-hover:opacity-100"
                        aria-hidden="true"
                      ></i>
                    </button>
                  {/each}
                </div>

                <!-- Today line -->
                {#each dates as date, index}
                  {#if isToday(date)}
                    <div
                      class="pointer-events-none absolute bottom-0 top-0 z-10 w-px bg-error"
                      style={`left: ${index * DAY_WIDTH + DAY_WIDTH / 2}px;`}
                    ></div>
                  {/if}
                {/each}

                <!-- Reservations -->
                {#each getRoomReservations(room.id, reservations) as reservation (reservation.id)}
                  <div
                    class="absolute top-1 z-30 h-[56px] hover:z-50"
                    style={getReservationStyle(reservation)}
                  >
                    <div
                      role="button"
                      tabindex="0"
                      class={`group relative flex h-full w-full cursor-pointer flex-col justify-center overflow-visible rounded-md px-3 text-left shadow-sm transition hover:brightness-95 hover:ring-3 hover:ring-info ${
                        hasReservationConflict(reservation, reservations)
                          ? "bg-error text-error-content ring-2 ring-error ring-offset-1"
                          : getStatusClass(reservation.status)
                      }`}
                      onclick={() => handleReservationClick(reservation)}
                      onkeydown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();

                          handleReservationClick(reservation);
                        }
                      }}
                    >
                      <!-- Reservation tooltip -->
                      <div
                        class={`pointer-events-none absolute left-1/2 z-50 w-64 -translate-x-1/2 cursor-default whitespace-normal rounded-lg border border-base-300 bg-base-100 p-4 text-left text-base-content opacity-0 shadow-xl transition-opacity duration-150 group-hover:pointer-events-auto group-hover:opacity-100 ${
                          roomIndex < TOOLTIP_BELOW_ROWS
                            ? "top-full mt-2"
                            : "bottom-full mb-2"
                        }`}
                      >
                        <div
                          class="mb-3 flex items-center gap-2 border-b border-base-300 pb-2"
                        >
                          <i class="fas fa-calendar-check text-base-content/60"></i>

                          <span class="font-bold">
                            Soba {room.roomNumber}
                          </span>
                        </div>

                        <div class="space-y-2 text-sm">
                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Rezervacija </span>
                            <span class="font-medium">
                              {reservation.id}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Gost </span>

                            <span class="font-medium">
                              <i class="fas fa-user mr-1 text-xs text-base-content/50"></i>
                              {reservation.bookerId}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Check-in </span>

                            <span class="font-medium">
                              {formatDate(parseDate(reservation.checkIn))}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Check-out </span>

                            <span class="font-medium">
                              {formatDate(parseDate(reservation.checkOut))}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Noći </span>

                            <span class="font-medium">
                              {getNights(reservation.checkIn, reservation.checkOut)}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Ukupno </span>

                            <span class="font-semibold">
                              € {Number(reservation.totalPrice ?? 0).toFixed(2)}
                            </span>
                          </div>

                          <div class="flex items-center justify-between gap-4">
                            <span class="text-base-content/60"> Status </span>

                            <span
                              class={`font-bold uppercase tracking-wide ${getReservationStatusColor(reservation.status)}`}
                            >
                              {getStatusLabel(reservation.status)}
                            </span>
                          </div>

                          {#if reservation.notes}
                            <div class="border-t border-base-300 pt-2">
                              <i class="fas fa-circle-info text-info/40"></i>
                              <span class="text-base-content/60">Napomena</span>

                              <p class="mt-1 font- font-bold">
                                {reservation.notes}
                              </p>
                            </div>
                          {/if}
                        </div>
                      </div>

                      {#if reservationStartsBeforeCalendar(reservation)}
                        <span
                          class="absolute left-1 top-1 text-xs"
                          aria-label="Rezervacija je počela prije prikazanog razdoblja"
                        >
                          {CONTINUES_BEFORE_SYMBOL}
                        </span>
                      {/if}

                      {#if reservationEndsAfterCalendar(reservation)}
                        <span
                          class="absolute right-1 top-1 text-xs"
                          aria-label="Rezervacija završava nakon prikazanog razdoblja"
                        >
                          {CONTINUES_AFTER_SYMBOL}
                        </span>
                      {/if}

                      <span class="truncate text-sm font-bold">
                        {reservation.bookerId}
                      </span>

                      <span class="truncate text-xs opacity-80">
                        {formatDate(parseDate(reservation.checkIn))}
                        →
                        {formatDate(parseDate(reservation.checkOut))}
                      </span>
                    </div>
                  </div>
                {/each}
              </div>
            </div>
          {/each}
        </div>
      </div>
    {/key}

    <!-- Legend -->
    <div class="flex flex-wrap gap-4 text-sm">
      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-warning"></span>
        Na čekanju
      </div>

      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-accent"></span>
        Potvrđena
      </div>

      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-success"></span>
        Prijavljen
      </div>

      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-neutral"></span>
        Odjavljen
      </div>

      <div class="flex items-center gap-2">
        <span class="h-3 w-3 rounded bg-error"></span>
        Otkazana
      </div>
    </div>
  {/if}

  <!-- ------------------------------------------------ -->
  <!-- Reservation modal -->
  <!-- ------------------------------------------------ -->

  {#if selectedReservation}
    {@const room = getRoomById(selectedReservation.roomId)}

    <div class="modal modal-open glassdrop" role="dialog" aria-modal="true">
      <div class="modal-box max-w-2xl bg-base-200 p-0 rounded-2xl">
        <!-- Header -->
        <div
          class="flex items-center justify-between border-b border-base-300 bg-base-300 px-6 py-4"
        >
          <div>
            <h3 class="text-lg font-bold">
              Rezervacija #{selectedReservation.id}
            </h3>
          </div>

          <button
            type="button"
            class="btn btn-sm btn-ghost btn-circle text-base-content/60 hover:text-primary"
            aria-label="Zatvori"
            onclick={closeModal}
          >
            <i class="fas fa-xmark"></i>
          </button>
        </div>

        <!-- Body -->
        <div class="p-8">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <!-- Room -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-door-open text-xs text-gray-400 mr-1"></i>
                  Soba
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={selectedReservation.roomNumber}
                disabled
              />
            </label>

            <!-- Guest -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-user text-xs text-gray-400 mr-1"></i>
                  Gost
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={selectedReservation.bookerId}
                disabled
              />
            </label>

            <!-- Check-in -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-calendar-check text-xs text-gray-400 mr-1"
                  ></i>
                  Check-in
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={formatDate(parseDate(selectedReservation.checkIn))}
                disabled
              />
            </label>

            <!-- Check-out -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-calendar-xmark text-xs text-gray-400 mr-1"
                  ></i>
                  Check-out
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={formatDate(parseDate(selectedReservation.checkOut))}
                disabled
              />
            </label>

            <!-- Status -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-info-circle text-xs text-gray-400 mr-1"></i>
                  Status
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={getStatusLabel(selectedReservation.status)}
                disabled
              />
            </label>

            <!-- Total price -->
            <label class="form-control">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-euro-sign text-xs text-gray-400 mr-1"></i>
                  Ukupna cijena (EUR)
                </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                value={`${Number(selectedReservation.totalPrice).toFixed(2)} €`}
                disabled
              />
            </label>

            <!-- Notes -->
            <label class="form-control md:col-span-2">
              <span class="label">
                <span class="label-text">
                  <i class="fas fa-sticky-note text-xs text-gray-400 mr-1"></i>
                  Napomena
                </span>
              </span>

              <textarea
                class="pgs-input"
                rows="3"
                value={selectedReservation.notes ?? ""}
                disabled
              ></textarea>
            </label>

            <!-- Open reservation -->
            <div class="flex items-end md:col-span-2">
              <a
                href={`#/reservations/${selectedReservation.id}`}
                use:link
                target="_blank"
                rel="noopener noreferrer"
                class="pgs-hyperlink"
              >
                <i class="fas fa-arrow-up-right-from-square mr-1"></i>
                Otvori detalje rezervacije
              </a>
            </div>
          </div>

          <div class="mt-6 flex justify-end">
            <button type="button" class="btn btn-ghost" onclick={closeModal}>
              Zatvori
            </button>
          </div>
        </div>
      </div>

      <button class="modal-backdrop" aria-label="Zatvori" onclick={closeModal}
      ></button>
    </div>
  {/if}

  <!-- ------------------------------------------------ -->
  <!-- New reservation modal -->
  <!-- ------------------------------------------------ -->

  {#if showNewModal}
    {@const nights = getNights(newCheckIn, newCheckOut)}

    {@const isNew = newCheckIn == "" && newCheckOut == ""}

    {@const invalidDates = !isNew && nights < 1}

    {@const conflict =
      newRoomId !== null &&
      !invalidDates &&
      hasNewReservationConflict(newRoomId, newCheckIn, newCheckOut)}

    <div class="modal modal-open glassdrop" role="dialog" aria-modal="true">
      <div class="modal-box max-w-2xl bg-base-200 p-0 rounded-lg">
        <!-- Header -->
        <div
          class="flex items-center justify-between border-b border-base-300 bg-info/30 px-6 py-4"
        >
          <h2 class="text-lg font-bold">
            <i class="fas fa-calendar-plus mr-2"></i>
            Nova rezervacija
          </h2>

          <button
            type="button"
            class="btn btn-sm btn-ghost btn-circle text-base-content/60 hover:text-primary"
            aria-label="Zatvori"
            onclick={closeModal}
          >
            <i class="fas fa-xmark"></i>
          </button>
        </div>

        <!-- Body -->
        <div class="p-8">
          <form
            class="grid grid-cols-1 gap-4 md:grid-cols-2"
            onsubmit={(e) => {
              e.preventDefault();

              if (!invalidDates && !conflict) {
                saveNewReservation();
              }
            }}
          >
            <!-- Room -->
            <label class="form-control md:col-span-2">
              <span class="label">
                <span class="label-text"> Soba </span>
              </span>

              <select
                class="pgs-input"
                value={newRoomId}
                onchange={(e) =>
                  handleNewRoomChange(Number(e.currentTarget.value))}
              >
                {#each rooms as r}
                  <option value={r.id}>
                    {r.roomNumber} — {r.type}
                    (€ {r.pricePerNight})
                  </option>
                {/each}
              </select>
            </label>

            <!-- Date From -->
            <label class="form-control">
              <span class="label">
                <span class="label-text"> Datum od </span>
              </span>

              <input type="date" class="pgs-input" bind:value={newCheckIn} />
            </label>

            <!-- Date To -->
            <label class="form-control">
              <span class="label">
                <span class="label-text"> Datum do </span>
              </span>

              <input type="date" class="pgs-input" bind:value={newCheckOut} />
            </label>

            <!-- Guest name -->
            <label class="form-control">
              <span class="label">
                <span class="label-text"> Ime </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                required
                bind:value={newGuestName}
              />
            </label>

            <!-- Guest surname -->
            <label class="form-control">
              <span class="label">
                <span class="label-text"> Prezime </span>
              </span>

              <input
                type="text"
                class="pgs-input"
                bind:value={newGuestSurname}
              />
            </label>

            <!-- Price per night -->
            <label class="form-control">
              <span class="label">
                <span class="label-text"> Cijena po noći (EUR) </span>
              </span>

              <input
                type="number"
                min="0"
                step="0.01"
                class="pgs-input"
                placeholder="0.00"
                bind:value={newPricePerNight}
              />
            </label>

            <!-- Total -->
            <div class="form-control">
              <span class="label w-full justify-end">
                <span class="label-text"> Ukupno </span>
              </span>

              <div
                class="flex h-full w-full items-center justify-end gap-2 px-1 text-right"
              >
                <span class="text-lg font-bold">
                  € {(Math.max(nights, 0) * (newPricePerNight || 0)).toFixed(2)}
                </span>

                <span class="text-sm opacity-60">
                  ({Math.max(nights, 0)}
                  {nights === 1 ? "noć" : "noći"})
                </span>
              </div>
            </div>

            <!-- Note -->
            <label class="form-control md:col-span-2">
              <span class="label">
                <span class="label-text"> Napomena </span>
              </span>

              <textarea class="pgs-input" rows="3" bind:value={newNote}
              ></textarea>
            </label>

            {#if invalidDates}
              <div class="text-sm text-error md:col-span-2">
                Datum do mora biti nakon datuma od.
              </div>
            {:else if conflict}
              <div class="text-sm text-error md:col-span-2">
                Soba je već zauzeta u odabranom razdoblju.
              </div>
            {/if}

            <!-- Buttons -->
            <div class="mt-2 flex justify-end gap-2 md:col-span-2">
              <button type="button" class="btn btn-ghost" onclick={closeModal}>
                <i class="fas fa-xmark"></i>
                Odustani
              </button>

              <button
                type="submit"
                class="btn btn-primary"
                disabled={invalidDates || conflict || saving}
              >
                {#if saving}
                  <span class="loading loading-spinner loading-sm"></span>
                {:else}
                  <i class="fas fa-check"></i>
                {/if}

                Spremi
              </button>
            </div>
          </form>
        </div>
      </div>

      <button class="modal-backdrop" aria-label="Zatvori" onclick={closeModal}
      ></button>
    </div>
  {/if}
</div>

<!-- TOUCH: window click closes the room tooltip, Escape closes it too -->
<svelte:window
  onclick={() => (openRoomTooltipId = null)}
  onkeydown={(e) => {
    if (e.key === "Escape") {
      openRoomTooltipId = null;
      closeModal();
    }
  }}
/>