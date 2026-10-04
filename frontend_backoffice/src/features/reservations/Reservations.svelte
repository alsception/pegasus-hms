<script lang="ts">
  import { onMount } from "svelte";
  import { link } from "svelte-spa-router";
  import { get } from "svelte/store";
  import { auth, getCurrentRole } from "../../core/services/SessionStore";
  import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
  import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";
  import type { Reservation } from "./Reservation";
  import { formatDate, formatDate2, formatDateTime } from "../../utils/formatting";
  import api from "../../core/services/client";


  type ReservationStatus =
    | "PENDING"
    | "CONFIRMED"
    | "CHECKED_IN"
    | "CHECKED_OUT"
    | "CANCELLED"
    | "NO_SHOW";


  let reservations: Reservation[] = [];
  let loading = false;
  let error = "";

  // Search fields
  let dateFrom = "";
  let dateTo = "";
  let priceFrom: number | null = null;
  let priceTo: number | null = null;
  let persons: number | null = null;

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  document.title = "Rezervacije | Pegasys HMS";

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


  //TODO: ove metode treba skloniti i koristiti iz format utils/formatting clase
  /* unction formatDate(date: string | null | undefined) 
  {
    console.log('formatting date:');
    console.log(date);
    if (!date) return "-";

    return new Intl.DateTimeFormat("hr-HR").format(
      new Date(`${date}T00:00:00`)
    );
  } */
 
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

  function getNights(from?: string, to?: string): number {
    if (!from || !to) return 0;
    const days = Math.round((new Date(to).getTime() - new Date(from).getTime()) / 86_400_000);
    return days > 0 ? days : 0;
  }

  function deleteReservation(id: number): any {
    handleDelete(id);
  }

  async function handleDelete(id: number) {
    if (!confirm("Obrisati rezervaciju "+id+"?")) return;

    loading = true;

    try 
    {
      await api(`/reservations/${id}`, { method: "DELETE" });

      loadReservations();
      
    } 
    catch (err) 
    {
      alert((err as Error).message);
    } finally {
      loading = false;
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
      class="btn btn-accent"
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
            Traži
          </button>

        </div>

      </form>

    </div>

  </div>

  <!-- Loading -->
  {#if loading}

       <LoadingOverlay />

  {/if}
  <!-- Error -->
  {#if error}

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

    <!-- DESKTOP TABLE VIEW -->
<div class="hidden md:block w-full max-w-[2048px] overflow-x-auto rounded-lg align-middle text-center mx-auto mt-8 border border-primary/5">

  <table class="table table-zebra min-w-full divide-y divide-accent">

    <thead class="bg-base-300">
      <tr class="h-12">
        <th class="pgs-th">Broj</th>
        <th class="pgs-th">Soba</th>
        <th class="pgs-th">Gost</th>
        <th class="pgs-th">Status</th>
        <th class="pgs-th">Dolazak</th>
        <th class="pgs-th">Odlazak</th>
        <th class="pgs-th">Noćenja</th>
        <th class="pgs-th">Vreme dolaska</th>
        <th class="pgs-th">Vreme odlaska</th>
        <th class="pgs-th">Gostiju</th>
        <th class="pgs-th-r">Cena</th>
        <th class="pgs-th">Napomena</th>
        <th class="pgs-th">Kreirano</th>
        <th class="pgs-th">Izmenjeno</th>
        <th class="pgs-th">Akcije</th>
      </tr>
    </thead>

    <tbody>
      {#each reservations as reservation, i}
        <tr
          class={`tr-highlight ${
            i % 2 === 1
              ? "bg-base-200"
              : "bg-base-100/60"
          }`}
        >

          <!-- ID -->
          <td class="pgs-td">
            <a
              use:link
              href="#/reservations/{reservation.id}"
              class="pgs-hyperlink font-bold font-mono"
            >
              {reservation.id}
            </a>
          </td>

          <!-- Room -->
          <td class="pgs-td">
            <a
              use:link
              href="#/inventory/{reservation.roomId}"
              class="pgs-hyperlink font-semibold"
            >
              {reservation.roomNumber}
            </a>
          </td>

          <!-- Booker -->
          <td class="pgs-td font-mono">
            {#if reservation.bookerId}
              <i class="fas fa-user text-gray-400 mr-1"></i>
              #{reservation.bookerId}
            {:else}
              <span class="text-gray-400">-</span>
            {/if}
          </td>

          <!-- Status -->
          <td class="text-center">
            <span
              class={`badge badge-soft badge-${getStatusColor(reservation.status)} font-mono badge-sm uppercase`}
            >
              {getStatusLabel(reservation.status)}
            </span>
          </td>

          <!-- Check-in -->
          <td class="pgs-td font-mono">
            {formatDate2(reservation.checkIn)}
          </td>

          <!-- Check-out -->
          <td class="pgs-td font-mono">
            {formatDate2(reservation.checkOut)}
          </td>

          <!-- Nights -->
          <td class="pgs-td font-mono">
            <i class="fas fa-moon text-gray-400 mr-1"></i>
            {getNights(reservation.checkIn, reservation.checkOut)}
          </td>

          <!-- Expected arrival -->
          <td class="pgs-td font-mono">
            {formatTime(reservation.expectedArrivalTime)}
          </td>

          <!-- Expected departure -->
          <td class="pgs-td font-mono">
            {formatTime(reservation.expectedDepartureTime)}
          </td>

          <!-- Guests -->
          <td class="pgs-td font-mono">
            <i class="fas fa-users text-gray-400 mr-1"></i>
            {reservation.guests}
          </td>

          <!-- Price -->
          <td class="pgs-td-num font-mono font-bold text-right">
            {formatPrice(reservation.totalPrice)}
          </td>

          <!-- Notes -->
          <td class="pgs-td">
            {#if reservation.notes}
              <div class="tooltip tooltip-info" data-tip={reservation.notes}>
                <i class="fas fa-note-sticky text-gray-500"></i>
              </div>
            {:else}
              <span class="text-gray-400">-</span>
            {/if}
          </td>

          <!-- Created -->
          <td class="pgs-td font-mono">
            {@html formatDate(reservation.created, "NOVO", 15)}
          </td>

          <!-- Modified -->
          <td class="pgs-td font-mono">
            {@html formatDate(reservation.modified, "NOVO", 15)}
          </td>

          <!-- Actions -->
          <td class="px-2">
            <div class="flex justify-center items-center gap-2">

              <div class="tooltip tooltip-info group" data-tip="Edit">
                <a
                  class="px-4"
                  aria-label="Edit"
                  use:link
                  href="#/reservations/{reservation.id}"
                >
                  <i class="fas fa-pen text-gray-500 group-hover:text-sky-400 cursor-pointer"></i>
                </a>
              </div>

              {#if getCurrentRole() === "ADMIN"}
                <button
                  class="px-4 group"
                  aria-label="Delete"
                  on:click={() => deleteReservation(reservation.id)}
                >
                  <div class="tooltip tooltip-info" data-tip="Delete">
                    <i class="fas fa-times-circle text-gray-500 group-hover:text-red-400 cursor-pointer"></i>
                  </div>
                </button>
              {/if}

            </div>
          </td>

        </tr>
      {/each}
    </tbody>

  </table>

  <div
    class="nb-table-footer text-left bg-secondary w-full p-4"
    style="background-color: var(--color-base-200);"
  >
    Pronađeno rezervacija:
    <span class="font-bold font-mono text-xl text-primary">
      {reservations.length}
    </span>
  </div>

</div>


<!-- MOBILE CARD VIEW -->
<div class="md:hidden space-y-4 mt-8">

  {#each reservations as reservation}

    <div class="card bg-base-200 shadow w-full">
      <div class="card-body p-4">

        <!-- Header -->
        <div class="flex justify-between items-start">
          <div>
            <div class="text-sm text-base-content/50">Rezervacija</div>
            <a
              use:link
              href="#/reservations/{reservation.id}"
              class="pgs-hyperlink text-lg font-bold font-mono"
            >
              {reservation.id}
            </a>
          </div>

          <span
            class={`badge badge-soft badge-${getStatusColor(reservation.status)} font-mono badge-sm uppercase`}
          >
            {getStatusLabel(reservation.status)}
          </span>
        </div>

        <div class="divider my-1"></div>

        <!-- Details -->
        <div class="space-y-2 text-sm">

          <div class="flex justify-between">
            <span class="text-base-content/60">Soba</span>
            <a
              use:link
              href="#/inventory/{reservation.roomId}"
              class="pgs-hyperlink font-semibold"
            >
              {reservation.roomNumber}
            </a>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Gost</span>
            <span class="font-mono">
              {reservation.bookerId ? `#${reservation.bookerId}` : "-"}
            </span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Dolazak</span>
            <span class="font-mono">{formatDate2(reservation.checkIn)}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Odlazak</span>
            <span class="font-mono">{formatDate2(reservation.checkOut)}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Noćenja</span>
            <span class="font-mono">
              {getNights(reservation.checkIn, reservation.checkOut)}
            </span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Vreme dolaska</span>
            <span class="font-mono">{formatTime(reservation.expectedArrivalTime)}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Vreme odlaska</span>
            <span class="font-mono">{formatTime(reservation.expectedDepartureTime)}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Gostiju</span>
            <span class="font-mono">
              <i class="fas fa-users text-gray-400 mr-1"></i>
              {reservation.guests}
            </span>
          </div>

          <div class="flex justify-between">
            <span class="text-base-content/60">Cena</span>
            <span class="font-mono font-bold text-primary">
              {formatPrice(reservation.totalPrice)}
            </span>
          </div>

        </div>

        <!-- Notes -->
        {#if reservation.notes}
          <div class="mt-3 text-sm text-base-content/60">
            <i class="fas fa-note-sticky mr-1"></i>
            {reservation.notes}
          </div>
        {/if}

        <!-- Timestamps -->
        {#if reservation.created || reservation.modified}
          <div class="mt-3 text-xs text-secondary space-y-1">
            {#if reservation.created}
              <div class="flex items-center gap-2">
                <i class="fas fa-calendar-plus text-gray-400"></i>
                <span>Kreirano:</span>
                <span class="font-mono">{formatDateTime(reservation.created)}</span>
              </div>
            {/if}

            {#if reservation.modified}
              <div class="flex items-center gap-2">
                <i class="fas fa-edit text-gray-400"></i>
                <span>Izmenjeno:</span>
                <span class="font-mono">{formatDateTime(reservation.modified)}</span>
              </div>
            {/if}
          </div>
        {/if}

        <!-- Actions -->
        <div class="card-actions justify-end mt-2">
          <a
            href={`#/reservations/${reservation.id}`}
            use:link
            class="btn btn-sm btn-outline"
          >
            <i class="fas fa-pen"></i>
            Detalji
          </a>

          {#if getCurrentRole() === "ADMIN"}
            <button
              class="btn btn-sm btn-outline hover:text-error"
              on:click={() => deleteReservation(reservation.id)}
            >
              <i class="fas fa-trash"></i>
              Obriši
            </button>
          {/if}
        </div>

      </div>
    </div>

  {/each}

</div>
  {/if}

</div>