<script lang="ts">
  import { onMount } from "svelte";
  import { params, push } from "svelte-spa-router";
  import { auth } from "../../core/services/SessionStore";
  import Login from "../../core/auth/Login.svelte";
  import api from "../../core/services/client";
  import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
  import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";
  import { formatDateTime } from "../../utils/formatting";
  import { showSuccessToast } from "../../core/utils/toaster";
  import type { Reservation } from "./Reservation";

  interface RoomOption {
    id: number;
    roomNumber: string;
    type: string;
    capacity: number | null;
    pricePerNight: number | null;
  }

  interface UserOption {
    id: number;
    username?: string;
    email?: string;
    firstName?: string;
    lastName?: string;
  }

  let isAuthenticated = false;
  let loading = false;
  let error: string | null = null;
  let ID: number | string = 0;

  let rooms: RoomOption[] = [];
  let users: UserOption[] = [];

  let formData: Partial<Reservation> = {
    roomId: null,
    bookerId: null,
    checkIn: "",
    checkOut: "",
    expectedArrivalTime: "",
    expectedDepartureTime: "",
    guests: 1,
    status: "PENDING",
    totalPrice: null,
    notes: ""
  };

  // IMPORTANT:
  // Replace these values with the exact constants from PGSReservationStatus.
  const reservationStatuses = [
    { value: "PENDING", label: "Na čekanju" },
    { value: "CONFIRMED", label: "Potvrđena" },
    { value: "CHECKED_IN", label: "Prijavljen" },
    { value: "CHECKED_OUT", label: "Odjavljen" },
    { value: "CANCELLED", label: "Otkazana" }
  ];

  document.title = "Reservation details: | Barbacoa";

  $: isAuthenticated = $auth.isAuthenticated;

  $: {
    if ($params?.id) {


      if($params?.id === 'new') 
      {
        ID = 0;
        console.log()
      }
      else
      {
        ID = Number($params.id);
      }

      

      // id=0 means create new reservation
      if (ID != 0) {
        fetchReservation(ID);
      }
    }
  }

  // Derived values
  $: selectedRoom = rooms.find((r) => r.id === Number(formData.roomId)) ?? null;

  $: nights = calcNights(formData.checkIn, formData.checkOut);

  $: suggestedPrice =
    selectedRoom?.pricePerNight != null && nights > 0
      ? Math.round(selectedRoom.pricePerNight * nights * 100) / 100
      : null;

  $: datesInvalid =
    !!formData.checkIn && !!formData.checkOut && formData.checkOut <= formData.checkIn;

  $: overCapacity =
    selectedRoom?.capacity != null &&
    formData.guests != null &&
    formData.guests > selectedRoom.capacity;

  function calcNights(from?: string, to?: string): number {
    if (!from || !to) return 0;

    const ms = new Date(to).getTime() - new Date(from).getTime();
    const days = Math.round(ms / 86_400_000);

    return days > 0 ? days : 0;
  }

  function toTimeInput(value?: string | null): string {
    // Backend LocalTime can be "14:00:00" -> input[type=time] wants "14:00" (both work, this is cleaner)
    return value ? value.substring(0, 5) : "";
  }

  function userLabel(u: UserOption): string {
    const fullName = [u.firstName, u.lastName].filter(Boolean).join(" ");
    return fullName || u.username || u.email || `#${u.id}`;
  }

  async function fetchReservation(id: string | number) {
    loading = true;
    error = null;

    try {
      const data = await api<Reservation>(`/reservations/${id}`, {
        method: "GET"
      });

      formData = {
        ...data,
        expectedArrivalTime: toTimeInput(data.expectedArrivalTime),
        expectedDepartureTime: toTimeInput(data.expectedDepartureTime)
      };
    } catch (err) {
      error = (err as Error).message;
    } finally {
      loading = false;
    }
  }

  async function fetchRooms() {
    try {
      rooms = await api<RoomOption[]>("/rooms", { method: "GET" });
    } catch (err) {
      error = (err as Error).message;
    }
  }

  async function fetchUsers() {
    try {
      users = await api<UserOption[]>("/users", { method: "GET" });
    } catch {
      // Not critical - form falls back to a plain ID input
      users = [];
    }
  }

  onMount(async () => {
    await Promise.all([fetchRooms(), fetchUsers()]);
  });

  function applySuggestedPrice() {
    if (suggestedPrice != null) {
      formData.totalPrice = suggestedPrice;
    }
  }

  async function handleSubmit() {
    if (datesInvalid) {
      alert("Datum odlaska mora biti nakon datuma dolaska.");
      return;
    }

    loading = true;

    try {
      const isNew = !ID || ID === 0;

      const payload = {
        ...formData,
        // empty strings -> null so Jackson doesn't choke on LocalTime
        expectedArrivalTime: formData.expectedArrivalTime || null,
        expectedDepartureTime: formData.expectedDepartureTime || null,
        bookerId: formData.bookerId || null
      };

      await api<Reservation>(isNew ? "/reservations" : `/reservations/${ID}`, {
        method: isNew ? "POST" : "PUT",
        body: JSON.stringify(payload)
      });

      showSuccessToast(isNew ? "Rezervacija je kreirana" : "Rezervacija je sačuvana");

      push("/reservations");
    } catch (err) {
      alert((err as Error).message);
    } finally {
      loading = false;
    }
  }

  async function handleDelete() {
    if (!confirm("Obrisati ovu rezervaciju?")) return;

    loading = true;

    try {
      await api(`/reservations/${ID}`, { method: "DELETE" });

      showSuccessToast("Rezervacija je obrisana");

      push("/reservations");
    } catch (err) {
      alert((err as Error).message);
    } finally {
      loading = false;
    }
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.ctrlKey && event.key === "Enter") {
      event.preventDefault();
      handleSubmit();
    }
  }

  function cancelEditing() {
    push("/reservations");
  }
</script>

<div class="relative w-full h-full scale-up-center-normal">
  {#if !$auth.isAuthenticated}
    <Login />

  {:else if loading}
    <LoadingOverlay />

  {:else if error}
    <ErrorDiv {error} />

  {:else}
    <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
    <form
      on:submit|preventDefault={handleSubmit}
      on:keydown={handleKeydown}
      id="reservationForm"
      class="max-w-8xl mx-auto bg-base-100 border border-primary/10 rounded-lg p-8 w-full space-y-8"
    >
      <!-- Header -->
      <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h3 class="text-3xl font-semibold text-primary">
            {ID && ID !== 0 ? "Rezervacija: "+ ID : "Nova rezervacija"}
          </h3>

          <p class="text-secondary text-sm uppercase tracking-wider mt-1">
            {ID && ID !== 0 ? "Uređivanje rezervacije" : ""}
          </p>
        </div>

        <div class="flex gap-3">
          <button
            type="button"
            on:click={cancelEditing}
            class="btn btn-outline"
          >
            <i class="fas fa-arrow-left text-primary/60"></i>
            Nazad
          </button>

          {#if ID && ID !== 0}
            <button
              type="button"
              class="btn btn-outline hover:text-error"
              on:click={handleDelete}
            >
              <i class="fas fa-trash text-primary/60"></i>
              Obriši
            </button>
          {/if}

          <button type="submit" class="btn btn-primary px-8">
            <i class="far fa-save text-primary-content"></i>
            Spremi
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <!-- Left column -->
        <div class="lg:col-span-7 space-y-8">

          <!-- Boravak -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6">
              <h3 class="text-xl font-semibold text-primary">
                Boravak
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Soba, termin i broj gostiju
              </p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Soba -->
              <div class="md:col-span-2">
                <label for="roomId" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-door-open text-xs text-gray-400 mr-1"></i>
                  Soba
                </label>

                <select
                  id="roomId"
                  class="pgs-input w-full"
                  bind:value={formData.roomId}
                  required
                >
                  <option value={null} disabled>Odaberi sobu</option>

                  {#each rooms as room}
                    <option value={room.id}>
                      {room.roomNumber} ({room.type}{room.capacity ? `, max ${room.capacity}` : ""})
                    </option>
                  {/each}
                </select>
              </div>

              <!-- Dolazak -->
              <div>
                <label for="checkIn" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-calendar-check text-xs text-gray-400 mr-1"></i>
                  Dolazak
                </label>

                <input
                  id="checkIn"
                  type="date"
                  class="pgs-input w-full"
                  bind:value={formData.checkIn}
                  required
                />
              </div>

              <!-- Odlazak -->
              <div>
                <label for="checkOut" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-calendar-minus text-xs text-gray-400 mr-1"></i>
                  Odlazak
                </label>

                <input
                  id="checkOut"
                  type="date"
                  class="pgs-input w-full"
                  class:border-error={datesInvalid}
                  min={formData.checkIn || undefined}
                  bind:value={formData.checkOut}
                  required
                />

                {#if datesInvalid}
                  <p class="text-error text-xs mt-2">
                    Datum odlaska mora biti nakon datuma dolaska.
                  </p>
                {/if}
              </div>

              <!-- Očekivani dolazak -->
              <div>
                <label for="arrivalTime" class="block text-sm font-medium text-secondary mb-2">
                  <i class="far fa-clock text-xs text-gray-400 mr-1"></i>
                  Očekivano vrijeme dolaska
                </label>

                <input
                  id="arrivalTime"
                  type="time"
                  class="pgs-input w-full"
                  bind:value={formData.expectedArrivalTime}
                />
              </div>

              <!-- Očekivani odlazak -->
              <div>
                <label for="departureTime" class="block text-sm font-medium text-secondary mb-2">
                  <i class="far fa-clock text-xs text-gray-400 mr-1"></i>
                  Očekivano vrijeme odlaska
                </label>

                <input
                  id="departureTime"
                  type="time"
                  class="pgs-input w-full"
                  bind:value={formData.expectedDepartureTime}
                />
              </div>

              <!-- Gosti -->
              <div>
                <label for="guests" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-users text-xs text-gray-400 mr-1"></i>
                  Broj gostiju
                </label>

                <input
                  id="guests"
                  type="number"
                  class="pgs-input w-full"
                  class:border-warning={overCapacity}
                  min="1"
                  bind:value={formData.guests}
                  required
                />

                {#if overCapacity}
                  <p class="text-warning text-xs mt-2">
                    Soba ima kapacitet za {selectedRoom?.capacity} gostiju.
                  </p>
                {/if}
              </div>

            </div>
          </div>

          <!-- Cijena -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6">
              <h3 class="text-xl font-semibold text-primary">
                Cijena
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Dogovorena ukupna cijena
              </p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Ukupna cijena -->
              <div>
                <label for="totalPrice" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-money-bill-wave text-xs text-gray-400 mr-1"></i>
                  Ukupna cijena EUR
                </label>

                <input
                  id="totalPrice"
                  type="number"
                  step="0.01"
                  min="0"
                  class="pgs-input w-full"
                  bind:value={formData.totalPrice}
                />
              </div>

              <!-- Prijedlog cijene -->
              <div>
                <span class="block text-sm font-medium text-secondary mb-2">
                  Izračun po cijeni sobe
                </span>

                {#if suggestedPrice != null}
                  <div class="flex items-center gap-3 pt-1">
                    <span class="font-mono text-primary/80">
                      {nights} × {selectedRoom?.pricePerNight} = {suggestedPrice} EUR
                    </span>

                    <button
                      type="button"
                      class="btn btn-sm btn-outline"
                      on:click={applySuggestedPrice}
                    >
                      Primijeni
                    </button>
                  </div>
                {:else}
                  <p class="text-secondary text-xs pt-2">
                    Odaberi sobu i datume za izračun.
                  </p>
                {/if}
              </div>

            </div>
          </div>

          <!-- Napomena -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <h3 class="text-xl font-semibold text-primary mb-6">
              Napomena
            </h3>

            <label for="notes" class="block text-sm font-medium text-secondary mb-2">
              Dodatne informacije
            </label>

            <textarea
              id="notes"
              class="pgs-input w-full resize-vertical"
              rows="6"
              maxlength="1000"
              bind:value={formData.notes}
              placeholder=""
            ></textarea>

            <p class="text-secondary text-xs mt-2 text-right">
              {(formData.notes ?? "").length} / 1000
            </p>
          </div>

        </div>

        <!-- Right column -->
        <div class="lg:col-span-5 space-y-8">

          <!-- Status rezervacije -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6 border-b border-neutral/10 pb-2">
              <h3 class="text-xl font-semibold text-primary">
                Status rezervacije
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Trenutno stanje
              </p>
            </div>

            <div>
              <label for="status" class="block text-sm font-medium text-secondary mb-2">
                <i class="fas fa-info-circle text-xs text-gray-400 mr-1"></i>
                Status
              </label>

              <select
                id="status"
                class="pgs-input w-full"
                bind:value={formData.status}
                required
              >
                {#each reservationStatuses as reservationStatus}
                  <option value={reservationStatus.value}>
                    {reservationStatus.label}
                  </option>
                {/each}
              </select>
            </div>

            <div class="mt-6">
              <div class="flex items-center gap-2">
                <span class="text-sm text-secondary">Trenutno:</span>

                <span
                  class="badge badge-outline"
                  class:border-success={formData.status === "CONFIRMED" || formData.status === "CHECKED_IN"}
                  class:text-success={formData.status === "CONFIRMED" || formData.status === "CHECKED_IN"}
                  class:border-warning={formData.status === "PENDING"}
                  class:text-warning={formData.status === "PENDING"}
                  class:border-error={formData.status === "CANCELLED"}
                  class:text-error={formData.status === "CANCELLED"}
                >
                  {reservationStatuses.find((s) => s.value === formData.status)?.label ?? formData.status}
                </span>
              </div>
            </div>
          </div>

          <!-- Gost -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6 border-b border-neutral/10 pb-2">
              <h3 class="text-xl font-semibold text-primary">Gost</h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Osoba koja je rezervisala
              </p>
            </div>

            <label for="bookerId" class="block text-sm font-medium text-secondary mb-2">
              <i class="fas fa-user text-xs text-gray-400 mr-1"></i>
              Korisnik
            </label>

            {#if users.length > 0}
              <select
                id="bookerId"
                class="pgs-input w-full"
                bind:value={formData.bookerId}
              >
                <option value={null}>Bez korisnika</option>

                {#each users as user}
                  <option value={user.id}>{userLabel(user)}</option>
                {/each}
              </select>
            {:else}
              <input
                id="bookerId"
                type="number"
                min="1"
                class="pgs-input w-full"
                placeholder="ID korisnika (opcionalno)"
                bind:value={formData.bookerId}
              />
            {/if}
          </div>

          <!-- Sažetak -->
          {#if selectedRoom && nights > 0}
            <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
              <div class="mb-6 border-b border-neutral/10 pb-2">
                <h3 class="text-xl font-semibold text-primary">Sažetak</h3>
              </div>

              <dl class="grid grid-cols-2 gap-y-3 text-sm">
                <dt class="text-secondary">Soba</dt>
                <dd class="font-mono text-right">{selectedRoom.roomNumber}</dd>

                <dt class="text-secondary">Noćenja</dt>
                <dd class="font-mono text-right">{nights}</dd>

                <dt class="text-secondary">Gostiju</dt>
                <dd class="font-mono text-right">{formData.guests ?? "-"}</dd>

                <dt class="text-secondary">Ukupno</dt>
                <dd class="font-mono text-right font-bold">
                  {formData.totalPrice ?? suggestedPrice ?? "-"} EUR
                </dd>
              </dl>
            </div>
          {/if}

        </div>

        <!-- Footer timestamps -->
        {#if formData.created || formData.modified}
          <div class="lg:col-span-12 p-4 text-[14px] text-secondary flex flex-col lg:flex-row lg:items-center lg:justify-between gap-2 lg:gap-10">
            {#if formData.created}
              <div class="flex items-center gap-2">
                <i class="fas fa-calendar-plus text-gray-400"></i>
                <span>
                  Upisano:
                  <span class="font-mono">
                    {formatDateTime(formData.created)}
                  </span>
                </span>
              </div>
            {/if}

            {#if formData.modified}
              <div class="flex items-center gap-2">
                <i class="fas fa-edit text-gray-400"></i>
                <span>
                  Izmijenjeno:
                  <span class="font-mono">
                    {formatDateTime(formData.modified)}
                  </span>
                </span>
              </div>
            {/if}
          </div>
        {/if}

      </div>
    </form>
  {/if}
</div>