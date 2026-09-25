<script lang="ts">
  import { onMount } from "svelte";
  import { link } from "svelte-spa-router";
  import { get } from "svelte/store";
  import { auth } from "../../core/services/SessionStore";
  import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
  import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";

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

  // Search fields
  let dateFrom = "";
  let dateTo = "";
  let priceFrom: number | null = null;
  let priceTo: number | null = null;
  let persons: number | null = null;

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  onMount(async () => {
    await loadReservations();
  });

  async function loadReservations() {
    loading = true;
    error = "";

    const token = get(auth).token;

    try {
      const params = new URLSearchParams();

      if (dateFrom) {
        params.append("dateFrom", dateFrom);
      }

      if (dateTo) {
        params.append("dateTo", dateTo);
      }

      if (priceFrom != null) {
        params.append("priceFrom", priceFrom.toString());
      }

      if (priceTo != null) {
        params.append("priceTo", priceTo.toString());
      }

      if (persons != null) {
        params.append("persons", persons.toString());
      }

      const query = params.toString();

      const response = await fetch(
        `${API_BASE_URL}/reservations/search${query ? `?${query}` : ""}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        }
      );

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

  function resetSearch() {
    dateFrom = "";
    dateTo = "";
    priceFrom = null;
    priceTo = null;
    persons = null;

    loadReservations();
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


  <!-- Search -->
    <div class="w-full flex justify-center mb-8">

      <div class="w-full max-w-8xl p-8 bg-base-200 rounded-lg">

      <h2 class="card-title text-lg mb-2">
        <i class="fas fa-filter"></i>
        Pretraga rezervacija
      </h2>

      <form
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4"
        on:submit|preventDefault={loadReservations}
      >

        <!-- Date From -->
        <label class="form-control">
          <span class="label">
            <span class="label-text">Datum od</span>
          </span>

          <input
            type="date"
            class="pgs-input"
            bind:value={dateFrom}
          />
        </label>


        <!-- Date To -->
        <label class="form-control">
          <span class="label">
            <span class="label-text">Datum do</span>
          </span>

          <input
            type="date"
            class="pgs-input"
            bind:value={dateTo}
          />
        </label>


        <!-- Price From -->
        <label class="form-control">
          <span class="label">
            <span class="label-text">Cena od (EUR)</span>
          </span>

          <input
            type="number"
            min="0"
            step="1"
            class="pgs-input"
            placeholder="0.00"
            bind:value={priceFrom}
          />
        </label>


        <!-- Price To -->
        <label class="form-control">
          <span class="label">
            <span class="label-text">Cena do (EUR)</span>
          </span>

          <input
            type="number"
            min="0"
            step="1"
            class="pgs-input"
            placeholder="9999.99"
            bind:value={priceTo}
          />
        </label>


        <!-- Persons -->
        <label class="form-control">
          <span class="label">
            <span class="label-text">Broj osoba</span>
          </span>

          <input
            type="number"
            min="1"
            step="1"
            class="pgs-input"
            placeholder="2"
            bind:value={persons}
          />
        </label>


        <!-- Buttons -->
        <div class="sm:col-span-2 lg:col-span-5 flex gap-2 justify-end mt-2">

          <button
            type="button"
            class="btn btn-ghost"
            on:click={resetSearch}
          >
            <i class="fas fa-xmark"></i>
            Poništi
          </button>

          <button
            type="submit"
            class="btn btn-primary"
          >
            <i class="fas fa-search"></i>
            Pretraži
          </button>

        </div>

      </form>

    </div>

  </div>

  <!-- Loading -->
  {#if loading}

       <LoadingOverlay />

  <!-- Error -->
  {:else if error}

        <ErrorDiv {error} />

  <!-- Empty -->
  {:else if reservations.length === 0}

    <div class="card bg-base-200">

      <div class="card-body items-center text-center py-16">

        <i class="fas fa-calendar-xmark text-5xl text-base-content/20"></i>

        <h2 class="card-title mt-2">
          Nema rezervacija
        </h2>

        <p class="text-base-content/60">
          Nema rezervacija koje odgovaraju zadanim kriterijima.
        </p>

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