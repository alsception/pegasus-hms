<script lang="ts">
    import { onMount } from "svelte";
    import api from "../../core/services/client";
    import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
    import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";
    import type { Reservation } from "./Reservation";

    let error: string | null = null;
    let loading = true;    

    type Room = {
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

    const DAYS_TO_SHOW = 30;
    const ROOM_WIDTH = 110;
    const DAY_WIDTH = 120;
    const ROW_HEIGHT = 64;

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
            month: "2-digit"
        });
    }

    function formatWeekday(date: Date): string {
        return date.toLocaleDateString("hr-HR", {
            weekday: "short"
        });
    }

    function dateKey(date: Date): string {
        return date.toISOString().split("T")[0];
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

        date.setDate(
            date.getDate() + DAYS_TO_SHOW
        );

        date.setHours(0, 0, 0, 0);

        return date;
    }

    // --------------------------------------------------
    // Reservation positioning
    // --------------------------------------------------

    function getReservationStart(
        reservation: Reservation
    ): number {
        const checkIn = parseDate(
            reservation.checkIn
        );

        const calendarStart =
            getCalendarStart();

        /*
         * If the reservation starts before the
         * displayed period, start it at day 0.
         */
        const visibleStart =
            checkIn < calendarStart
                ? calendarStart
                : checkIn;

        const diff =
            visibleStart.getTime() -
            calendarStart.getTime();

        return Math.floor(
            diff / DAY_MS
        );
    }

    function getReservationEnd(
        reservation: Reservation
    ): number {
        const checkOut = parseDate(
            reservation.checkOut
        );

        const calendarStart =
            getCalendarStart();

        const calendarEnd =
            getCalendarEnd();

        /*
         * If checkout is after the displayed
         * period, stop it at the last visible day.
         */
        const visibleEnd =
            checkOut > calendarEnd
                ? calendarEnd
                : checkOut;

        const diff =
            visibleEnd.getTime() -
            calendarStart.getTime();

        return Math.ceil(
            diff / DAY_MS
        );
    }

    function getReservationDays(
        reservation: Reservation
    ): number {
        const start =
            getReservationStart(
                reservation
            );

        const end =
            getReservationEnd(
                reservation
            );

        return Math.max(
            1,
            end - start
        );
    }

    function getReservationStyle(
        reservation: Reservation
    ): string {
        const start =
            getReservationStart(
                reservation
            );

        const days =
            getReservationDays(
                reservation
            );

        return `
            left: ${start * DAY_WIDTH + 4}px;
            width: ${days * DAY_WIDTH - 8}px;
        `;
    }



    // --------------------------------------------------
    // Status
    // --------------------------------------------------

    function getStatusClass(
        status: ReservationStatus
    ): string {
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

    function getStatusLabel(
        status: ReservationStatus
    ): string {
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
                return "Otkazana";

            default:
                return status;
        }
    }

    // --------------------------------------------------
    // Reservation filtering
    // --------------------------------------------------

    function getRoomReservations(
        roomId: number
    ): Reservation[] {
        return reservations.filter(
            reservation =>
                reservation.roomId === roomId
        );
    }

    function hasReservationConflict(
        reservation: Reservation
    ): boolean {
        const checkIn = parseDate(reservation.checkIn);
        const checkOut = parseDate(reservation.checkOut);

        return reservations.some(other => {
            // Same reservation
            if (other.id === reservation.id) {
                return false;
            }

            // Different room
            if (other.roomId !== reservation.roomId) {
                return false;
            }

            const otherCheckIn = parseDate(other.checkIn);
            const otherCheckOut = parseDate(other.checkOut);

            let conflict = (
                checkIn < otherCheckOut &&
                checkOut > otherCheckIn
            );

            if(conflict) console.error('Conflict reservation',reservation);

            return conflict;
        });
    }

    // --------------------------------------------------
    // Navigation
    // --------------------------------------------------

    function changePeriod(days: number) {
        const newStartDate =
            new Date(startDate);

        newStartDate.setDate(
            newStartDate.getDate() + days
        );

        newStartDate.setHours(
            0,
            0,
            0,
            0
        );

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
        const newStartDate =
            new Date();

        newStartDate.setHours(
            0,
            0,
            0,
            0
        );

        startDate = newStartDate;

        generateDates();
    }

    // --------------------------------------------------
    // Events
    // --------------------------------------------------

    function handleReservationClick(
        reservation: Reservation
    ) {
        console.log(
            "Reservation:",
            reservation
        );
    }

    function handleEmptyCellClick(
        room: Room,
        date: Date
    ) {
        console.log(
            "New reservation:",
            room.roomNumber,
            dateKey(date)
        );
    }

    // --------------------------------------------------
    // API
    // --------------------------------------------------

    async function fetchRooms() {
        try {
            rooms = await api<Room[]>(
                "/rooms",
                {
                    method: "GET"
                }
            );
        } catch (err) {
            error =
                (err as Error).message;
        }
    }

    async function fetchReservations() {
        try {
            reservations =
                await api<Reservation[]>(
                    "/reservations",
                    {
                        method: "GET"
                    }
                );
        } catch (err) {
            error =
                (err as Error).message;
        }
    }

    // --------------------------------------------------
    // Initial setup
    // --------------------------------------------------

    onMount(async () => {
        try {
            generateDates();

            await Promise.all([
                fetchRooms(),
                fetchReservations()
            ]);
        } finally {
            loading = false;

            console.log(reservations);
            console.log(rooms);
        }
    });
</script>


<div class="flex flex-col gap-4">

    <!-- ------------------------------------------------ -->
    <!-- Toolbar -->
    <!-- ------------------------------------------------ -->

    <div
        class="flex flex-wrap items-center justify-between gap-2"
    >

        <div>
            <h2 class="text-xl font-bold">
                Rezervacije
            </h2>

            <p class="text-sm text-base-content/60">
                Pregled zauzetosti soba
            </p>
        </div>


        <div class="join">

            <button
                class="btn btn-sm join-item"
                onclick={previousPeriod}
            >
                ‹
            </button>

            <button
                class="btn btn-sm join-item"
                onclick={today}
            >
                Danas
            </button>

            <button
                class="btn btn-sm join-item"
                onclick={nextPeriod}
            >
                ›
            </button>

        </div>

    </div>


    {#if loading}

        <LoadingOverlay />

    {:else if error}

        <ErrorDiv {error} />

    {:else}

    {#key startDate.getTime()}

        <!-- ------------------------------------------------ -->
        <!-- Board -->
        <!-- ------------------------------------------------ -->

        <div
            class="overflow-auto rounded-lg border border-base-300 bg-base-100"
        >
            <div
                class="relative"
                style={`width: ${
                    ROOM_WIDTH +
                    dates.length *
                        DAY_WIDTH
                }px;`}
            >

                <!-- ====================================== -->
                <!-- HEADER -->
                <!-- ====================================== -->

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
                                isToday(date)
                                    ? "bg-primary text-primary-content"
                                    : "bg-base-200"
                            }`}
                            style={`width: ${DAY_WIDTH}px;`}
                        >

                            <span
                                class="text-xs uppercase"
                            >
                                {formatWeekday(
                                    date
                                )}
                            </span>

                            <span class="font-bold">
                                {formatDate(
                                    date
                                )}
                            </span>

                        </div>

                    {/each}

                </div>


                <!-- ====================================== -->
                <!-- ROOMS -->
                <!-- ====================================== -->

                {#each rooms as room}

                    <div
                        class="relative flex"
                        style={`height: ${ROW_HEIGHT}px;`}
                    >

                        <!-- Room -->

                        <div
                            class="sticky left-0 z-21 flex shrink-0 flex-col justify-center border-b border-r border-base-300 bg-base-100 px-3"
                            style={`width: ${ROOM_WIDTH}px;`}
                        >

                            <span class="font-bold">
                                {room.roomNumber}
                            </span>

                            <span
                                class="text-xs text-base-content/50"
                            >
                                {room.type}
                            </span>

                            <span
                                class="text-xs text-base-content/50"
                            >
                                € {room.pricePerNight} <i class="fas fa-user w-3"></i> {room.capacity}
                            </span>

                        </div>


                        <!-- Calendar -->

                        <div
                            class="relative shrink-0"
                            style={`width: ${
                                dates.length *
                                DAY_WIDTH
                            }px;`}
                        >

                            <!-- Grid -->

                            <div
                                class="absolute inset-0 flex"
                            >

                                {#each dates as date}

                                    <button
                                        class={`h-full shrink-0 border-b border-r border-base-300 transition hover:bg-base-200 ${
                                            isToday(
                                                date
                                            )
                                                ? "bg-primary/5"
                                                : ""
                                        }`}
                                        style={`width: ${DAY_WIDTH}px;`}
                                        aria-label={`Nova rezervacija za sobu ${room.roomNumber} ${formatDate(date)}`}
                                        onclick={() =>
                                            handleEmptyCellClick(
                                                room,
                                                date
                                            )}
                                    ></button>

                                {/each}

                            </div>


                            <!-- Today line -->

                            {#each dates as date, index}

                                {#if isToday(date)}

                                    <div
                                        class="pointer-events-none absolute bottom-0 top-0 z-10 w-px bg-error"
                                        style={`left: ${
                                            index *
                                                DAY_WIDTH +
                                            DAY_WIDTH /
                                                2
                                        }px;`}
                                    ></div>

                                {/if}

                            {/each}


                            <!-- Reservations -->

                            {#each getRoomReservations(
                                room.id
                            ) as reservation}

                                <button
                                    class={`absolute top-1 z-20 flex h-[56px] flex-col justify-center overflow-hidden rounded-md px-3 text-left shadow-sm transition hover:brightness-95 ${
                                        hasReservationConflict(reservation)
                                            ? "bg-error text-error-content ring-2 ring-error ring-offset-1"
                                            : getStatusClass(reservation.status)
                                    }`}
                                    style={getReservationStyle(
                                        reservation
                                    )}
                                    title={`${reservation.bookerId} — ${getStatusLabel(
                                        reservation.status
                                    )}`}
                                    onclick={() =>
                                        handleReservationClick(
                                            reservation
                                        )}
                                >

                                    <span
                                        class="truncate text-sm font-bold"
                                    >
                                        {reservation.bookerId}
                                    </span>

                                    <span
                                        class="truncate text-xs opacity-80"
                                    >
                                        {formatDate(
                                            parseDate(
                                                reservation.checkIn
                                            )
                                        )}
                                        →
                                        {formatDate(
                                            parseDate(
                                                reservation.checkOut
                                            )
                                        )}
                                    </span>

                                </button>

                            {/each}

                        </div>

                    </div>

                {/each}

            </div>

              </div>

    {/key}


    <!-- ------------------------------------------------ -->
    <!-- Legend -->
    <!-- ------------------------------------------------ -->

    <div
        class="flex flex-wrap gap-4 text-sm"
    >

            <div class="flex items-center gap-2">
                <span
                    class="h-3 w-3 rounded bg-warning"
                ></span>
                Na čekanju
            </div>

            <div class="flex items-center gap-2">
                <span
                    class="h-3 w-3 rounded bg-accent"
                ></span>
                Potvrđena
            </div>

            <div class="flex items-center gap-2">
                <span
                    class="h-3 w-3 rounded bg-success"
                ></span>
                Prijavljen
            </div>

            <div class="flex items-center gap-2">
                <span
                    class="h-3 w-3 rounded bg-neutral"
                ></span>
                Odjavljen
            </div>

            <div class="flex items-center gap-2">
                <span
                    class="h-3 w-3 rounded bg-error"
                ></span>
                Otkazana
            </div>

        </div>

    {/if}

</div>