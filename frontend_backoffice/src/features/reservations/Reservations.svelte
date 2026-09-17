<script lang="ts">
  import { onMount } from "svelte";
  import { link } from "svelte-spa-router";
  import { get } from "svelte/store";
  import { auth, getCurrentRole, isAdmin } from "../../core/services/SessionStore";

  type ReservationStatus =
    | "PENDING"
    | "CONFIRMED"
    | "CHECKED_IN"
    | "CHECKED_OUT"
    | "CANCELLED"
    | "NO_SHOW";

  interface Reservation {
    id: number;
    bookedByUserId: number | null;
    roomId: number;
    checkIn: string;
    checkOut: string;
    expectedArrivalTime: string | null;
    expectedDepartureTime: string | null;
    status: ReservationStatus;
    totalPrice: number | null;
    notes: string | null;
  }

  let reservations: Reservation[] = [];
  let loading = true;
  let error = "";

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  onMount(async () => {
    console.log('hello reservations');
    await loadReservations();
  });

  async function loadReservations() {
    loading = true;
    error = "";
        const token = get(auth).token;


    try {
        
      const response = await fetch(`${API_BASE_URL}/reservations`,{
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        });

      if (!response.ok) {
        throw new Error("Greška prilikom učitavanja rezervacija");
      }

      reservations = await response.json();
    } catch (err) {
      console.error(err);
      error = "Nije moguće učitati rezervacije.";
    } finally {
      loading = false;
    }
  }

  function formatDate(date: string) {
    return new Intl.DateTimeFormat("hr-HR").format(
      new Date(`${date}T00:00:00`)
    );
  }

  function formatTime(time: string | null) {
    if (!time) return "-";

    return time.substring(0, 5);
  }

  function formatPrice(price: number | null) {
    if (price == null) return "-";

    return new Intl.NumberFormat("hr-HR", {
      style: "currency",
      currency: "EUR"
    }).format(price);
  }

  function getStatusLabel(status: ReservationStatus) {
    switch (status) {
      case "PENDING":
        return "Na čekanju";
      case "CONFIRMED":
        return "Potvrđena";
      case "CHECKED_IN":
        return "Prijavljen";
      case "CHECKED_OUT":
        return "Odjavljen";
      case "CANCELLED":
        return "Otkazana";
      case "NO_SHOW":
        return "Nije došao";
      default:
        return status;
    }
  }

  function getStatusColor(status: ReservationStatus) {
    switch (status) {
      case "PENDING":
        return "warning";
      case "CONFIRMED":
        return "info";
      case "CHECKED_IN":
        return "success";
      case "CHECKED_OUT":
        return "neutral";
      case "CANCELLED":
        return "error";
      case "NO_SHOW":
        return "error";
      default:
        return "neutral";
    }
  }
</script>

<div class="p-4 md:p-6">

  <!-- Header -->
  <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">

    <div>
      <h1 class="text-2xl font-bold">
        Rezervacije
      </h1>

      <p class="text-base-content/60">
        Pregled hotelskih rezervacija
      </p>
    </div>

    <a
      href="#/reservations/new"
      use:link
      class="btn btn-primary"
    >
      <i class="fas fa-plus"></i>
      Nova rezervacija
    </a>

  </div>


  <!-- Loading -->
  {#if loading}

    <div class="flex justify-center py-16">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>


  <!-- Error -->
  {:else if error}

    <div class="alert alert-error">
      <i class="fas fa-circle-exclamation"></i>
      <span>{error}</span>

      <button
        class="btn btn-sm"
        on:click={loadReservations}
      >
        Pokušaj ponovo
      </button>
    </div>


  <!-- Empty -->
  {:else if reservations.length === 0}

    <div class="card bg-base-200">
      <div class="card-body items-center text-center py-16">

        <i class="fas fa-calendar-xmark text-5xl text-base-content/20"></i>

        <h2 class="card-title mt-2">
          Nema rezervacija
        </h2>

        <p class="text-base-content/60">
          Trenutno nema evidentiranih rezervacija.
        </p>

        <a
          href="#/reservations/new"
          use:link
          class="btn btn-primary mt-2"
        >
          Nova rezervacija
        </a>

      </div>
    </div>


  {:else}

    <!-- Desktop -->
    <div class="hidden md:block overflow-x-auto rounded-box border border-base-300">

      <table class="table table-zebra">

        <thead>
          <tr>
            <th>ID</th>
            <th>Soba</th>
            <th>Dolazak</th>
            <th>Odlazak</th>
            <th>Dolazak vreme</th>
            <th>Status</th>
            <th>Cena</th>
            <th></th>
          </tr>
        </thead>

        <tbody>

          {#each reservations as reservation}

            <tr>

              <td class="font-mono">
                #{reservation.id}
              </td>

              <td>
                <span class="font-semibold">
                  #{reservation.roomId}
                </span>
              </td>

              <td>
                {formatDate(reservation.checkIn)}
              </td>

              <td>
                {formatDate(reservation.checkOut)}
              </td>

              <td class="font-mono">
                {formatTime(reservation.expectedArrivalTime)}
              </td>

              <td>
                <span
                  class={`badge badge-soft badge-${getStatusColor(reservation.status)}`}
                >
                  {getStatusLabel(reservation.status)}
                </span>
              </td>

              <td class="font-mono font-semibold">
                {formatPrice(reservation.totalPrice)}
              </td>

              <td>
                <a
                  href={`#/reservations/${reservation.id}`}
                  use:link
                  class="btn btn-sm btn-ghost"
                >
                  <i class="fas fa-eye"></i>
                </a>
              </td>

            </tr>

          {/each}

        </tbody>

      </table>

    </div>


    <!-- Mobile -->
    <div class="md:hidden space-y-4">

      {#each reservations as reservation}

        <div class="card bg-base-200 shadow">

          <div class="card-body p-4">

            <div class="flex justify-between items-start">

              <div>
                <div class="text-sm text-base-content/50">
                  Rezervacija
                </div>

                <h2 class="text-lg font-bold">
                  #{reservation.id}
                </h2>
              </div>

              <span
                class={`badge badge-soft badge-${getStatusColor(reservation.status)}`}
              >
                {getStatusLabel(reservation.status)}
              </span>

            </div>

            <div class="divider my-1"></div>

            <div class="space-y-2 text-sm">

              <div class="flex justify-between">
                <span class="text-base-content/60">
                  Soba
                </span>

                <span class="font-semibold">
                  #{reservation.roomId}
                </span>
              </div>

              <div class="flex justify-between">
                <span class="text-base-content/60">
                  Dolazak
                </span>

                <span>
                  {formatDate(reservation.checkIn)}
                </span>
              </div>

              <div class="flex justify-between">
                <span class="text-base-content/60">
                  Odlazak
                </span>

                <span>
                  {formatDate(reservation.checkOut)}
                </span>
              </div>

              <div class="flex justify-between">
                <span class="text-base-content/60">
                  Vreme dolaska
                </span>

                <span class="font-mono">
                  {formatTime(reservation.expectedArrivalTime)}
                </span>
              </div>

              <div class="flex justify-between">
                <span class="text-base-content/60">
                  Cena
                </span>

                <span class="font-mono font-bold text-primary">
                  {formatPrice(reservation.totalPrice)}
                </span>
              </div>

            </div>

            {#if reservation.notes}

              <div class="mt-3 text-sm text-base-content/60">
                <i class="fas fa-note-sticky mr-1"></i>
                {reservation.notes}
              </div>

            {/if}

            <div class="card-actions justify-end mt-2">

              <a
                href={`#/reservations/${reservation.id}`}
                use:link
                class="btn btn-sm btn-outline"
              >
                <i class="fas fa-eye"></i>
                Detalji
              </a>

            </div>

          </div>

        </div>

      {/each}

    </div>

  {/if}

</div>