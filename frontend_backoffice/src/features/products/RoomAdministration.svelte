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

  interface Room {
    id?: number;
    roomNumber: string;
    type: string;
    status: string;
    floor: number | null;
    capacity: number | null;
    pricePerNight: number | null;
    description: string;
    active: boolean;
    created?: string;
    modified?: string;
  }

  let isAuthenticated = false;
  let loading = false;
  let error: string | null = null;
  let ID: number | string = 0;

  let formData: Partial<Room> = {
    roomNumber: "",
    type: "",
    status: "AVAILABLE",
    floor: null,
    capacity: null,
    pricePerNight: null,
    description: "",
    active: true
  };

  // IMPORTANT:
  // Replace these values with the exact constants from your Java enums.
  const roomTypes = [
    { value: "SINGLE", label: "Jednokrevetna" },
    { value: "DOUBLE", label: "Dvokrevetna" },
    { value: "TWIN", label: "Twin" },
    { value: "TRIPLE", label: "Trokrevetna" },
    { value: "SUITE", label: "Apartman / Suite" }
  ];

  const roomStatuses = [
    { value: "AVAILABLE", label: "Dostupna" },
    { value: "OCCUPIED", label: "Zauzeta" },
    { value: "RESERVED", label: "Rezervisana" },
    { value: "CLEANING", label: "Čišćenje" },
    { value: "MAINTENANCE", label: "Održavanje" },
    { value: "OUT_OF_ORDER", label: "Van funkcije" }
  ];

  document.title = "Room details: | Barbacoa";

  $: isAuthenticated = $auth.isAuthenticated;

  $: {
    if ($params?.id) {
      ID = Number($params.id);

      // id=0 means create new room
      if (ID != 0) {
        fetchRoom(ID);
      }
    }
  }

  async function fetchRoom(id: string | number) {
    loading = true;
    error = null;

    try {
      const data = await api<Room>(`/rooms/${id}`, {
        method: "GET"
      });

      formData = data;
    } catch (err) {
      error = (err as Error).message;
    } finally {
      loading = false;
    }
  }

  onMount(async () => {
    const queryParams = new URLSearchParams(window.location.search);
    const id = queryParams.get("id");

    if (id) {
      ID = Number(id);

      if (ID != 0) {
        await fetchRoom(ID);
      }
    }
  });

  async function handleSubmit() {
    loading = true;

    try {
      const isNew = !ID || ID === 0;

      await api<Room>(isNew ? "/rooms" : `/rooms/${ID}`, {
        method: isNew ? "POST" : "PUT",
        body: JSON.stringify(formData)
      });

      showSuccessToast(isNew ? "Soba je kreirana" : "Soba je sačuvana");

      push("/inventory");
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
    push("/inventory");
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
      id="roomForm"
      class="max-w-[100rem] mx-auto bg-base-100 border border-primary/10 rounded-lg p-8 w-full space-y-8"
    >
      <!-- Header -->
      <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h3 class="text-3xl font-semibold text-primary">
            Soba
          </h3>

          <p class="text-secondary text-sm uppercase tracking-wider mt-1">
            {ID && ID !== 0 ? "Uređivanje sobe" : "Nova soba"}
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
              on:click={() => alert("Implementirati brisanje sobe")}
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

          <!-- Osnovno -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6">
              <h3 class="text-xl font-semibold text-primary">
                Osnovno
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Osnovne informacije o sobi
              </p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Broj sobe -->
              <div>
                <label for="roomNumber" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-door-open text-xs text-gray-400 mr-1"></i>
                  Broj sobe
                </label>

                <input
                  id="roomNumber"
                  type="text"
                  class="pgs-input w-full"
                  placeholder="npr. 101"
                  bind:value={formData.roomNumber}
                  required
                />
              </div>

              <!-- Tip sobe -->
              <div>
                <label for="type" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-bed text-xs text-gray-400 mr-1"></i>
                  Tip sobe
                </label>

                <select
                  id="type"
                  class="pgs-input w-full"
                  bind:value={formData.type}
                  required
                >
                  <option value="" disabled>Odaberi tip sobe</option>

                  {#each roomTypes as roomType}
                    <option value={roomType.value}>
                      {roomType.label}
                    </option>
                  {/each}
                </select>
              </div>

              <!-- Sprat -->
              <div>
                <label for="floor" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-building text-xs text-gray-400 mr-1"></i>
                  Sprat
                </label>

                <input
                  id="floor"
                  type="number"
                  class="pgs-input w-full"
                  min="0"
                  bind:value={formData.floor}
                  required
                />
              </div>

              <!-- Kapacitet -->
              <div>
                <label for="capacity" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-users text-xs text-gray-400 mr-1"></i>
                  Kapacitet
                </label>

                <input
                  id="capacity"
                  type="number"
                  class="pgs-input w-full"
                  min="1"
                  bind:value={formData.capacity}
                  required
                />
              </div>

              <!-- Opis -->
              <div class="md:col-span-2">
                <label for="description" class="block text-sm font-medium text-secondary mb-2">
                  Opis
                </label>

                <textarea
                  id="description"
                  class="pgs-input w-full resize-vertical"
                  rows="4"
                  bind:value={formData.description}
                  placeholder="Dodatni opis sobe..."
                ></textarea>
              </div>

              <!-- Aktivna -->
              <div>
                <label for="active" class="block text-sm font-medium text-secondary mb-2">
                  Status zapisa
                </label>

                <div class="flex items-center space-x-3 pt-2">
                  <input
                    id="active"
                    type="checkbox"
                    bind:checked={formData.active}
                    class="text-sm toggle ring-2 ring-primary bg-base-100 text-gray-600 checked:text-green-600"
                  />

                  <p class="font-mono font-bold text-primary/80">
                    {#if formData.active}
                      <span class="text-green-600 text-lg">AKTIVNA</span>
                    {:else}
                      <span class="text-gray-600 text-lg">NEAKTIVNA</span>
                    {/if}
                  </p>
                </div>

                <p class="text-secondary text-xs mt-2">
                  Neaktivne sobe neće biti dostupne za korišćenje.
                </p>
              </div>

            </div>
          </div>

          <!-- Cena -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6">
              <h3 class="text-xl font-semibold text-primary">
                Cena
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Cena noćenja
              </p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Cena po noći -->
              <div>
                <label for="pricePerNight" class="block text-sm font-medium text-secondary mb-2">
                  <i class="fas fa-money-bill-wave text-xs text-gray-400 mr-1"></i>
                  Cena po noći
                </label>

                <input
                  id="pricePerNight"
                  type="number"
                  step="0.01"
                  min="0"
                  class="pgs-input w-full"
                  bind:value={formData.pricePerNight}
                  required
                />
              </div>

              <!-- Valuta - opciono, nije deo entiteta -->
              <div>
                <label class="block text-sm font-medium text-secondary mb-2">
                  Valuta
                </label>

                <input
                  type="text"
                  class="pgs-input w-full bg-base-300 opacity-60"
                  value="EUR"
                  disabled
                />

                <p class="text-secondary text-xs mt-2">
                  Valuta nije posebno polje u PGSRoom entitetu.
                </p>
              </div>

            </div>
          </div>

        </div>

        <!-- Right column -->
        <div class="lg:col-span-5 space-y-8">

          <!-- Status sobe -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <div class="mb-6 border-b border-neutral/10 pb-2">
              <h3 class="text-xl font-semibold text-primary">
                Status sobe
              </h3>
              <p class="text-secondary text-sm uppercase tracking-wider">
                Trenutna raspoloživost
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
                {#each roomStatuses as roomStatus}
                  <option value={roomStatus.value}>
                    {roomStatus.label}
                  </option>
                {/each}
              </select>
            </div>

            <div class="mt-6">
              <div class="flex items-center gap-2">
                <span class="text-sm text-secondary">Trenutno:</span>

                <span
                  class="badge badge-outline"
                  class:border-success={formData.status === "AVAILABLE"}
                  class:text-success={formData.status === "AVAILABLE"}
                >
                  {roomStatuses.find((s) => s.value === formData.status)?.label ?? formData.status}
                </span>
              </div>
            </div>
          </div>

          <!-- Napomena -->
          <div class="bg-base-200 p-6 rounded-xl shadow-sm border border-neutral/20">
            <h3 class="text-xl font-semibold text-primary mb-6">
              Napomena
            </h3>

            <label for="roomDescription" class="block text-sm font-medium text-secondary mb-2">
              Dodatne informacije
            </label>

            <textarea
              id="roomDescription"
              class="pgs-input w-full resize-vertical"
              rows="6"
              bind:value={formData.description}
              placeholder="Napomena o sobi, opremi, pogledu, dodatnim krevetima..."
            ></textarea>
          </div>

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
                  Izmenjeno:
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

<style>
  .toggle {
    background-color: rgb(57, 57, 36);
  }
</style>