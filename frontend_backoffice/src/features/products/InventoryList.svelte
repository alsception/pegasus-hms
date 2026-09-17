<script lang="ts">
  import { onMount } from "svelte";
  import { link } from "svelte-spa-router";
  import { get } from "svelte/store";

  import { auth, getCurrentRole, isAdmin } from "../../core/services/SessionStore";
  import { formatDate } from "../../utils/formatting";

  import Login from "../../core/auth/Login.svelte";
  import LoadingOverlay from "../../core/utils/LoadingOverlay.svelte";
  import ErrorDiv from "../../core/navigation/error/ErrorDiv.svelte";

  // Room entity representation
  interface Room {
    imageUrl: any;
    id: number;
    roomNumber: string;
    type: string;
    status: string;
    floor: number;
    capacity: number;
    pricePerNight: number;
    description?: string;
    active: boolean;
    created?: string;
    modified?: string;
  }

  document.title = "Sobe | Pegasus";

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  // DEFINITIONS
  let isAuthenticated = false;
  let rooms: Room[] = [];

  let loading = false;
  let error: string | null = null;

  let isListView = false;
  let isAdminView = true;
  let isDark = true;

  let page = 0;
  let size = 250;

  let totalRooms = 0;
  let totalPages = 0;

  let searchTerm = "";

  // Room types
  // IMPORTANT: Keep these values synchronized with PGSRoomType enum.
  const roomTypes: Record<string, string> = {
    SINGLE: "Jednokrevetna",
    DOUBLE: "Dvokrevetna",
    TWIN: "Twin",
    TRIPLE: "Trokrevetna",
    SUITE: "Apartman / Suite"
  };

  // Room statuses
  // IMPORTANT: Keep these values synchronized with PGSRoomStatus enum.
  const roomStatuses: Record<string, string> = {
    AVAILABLE: "Dostupna",
    OCCUPIED: "Zauzeta",
    RESERVED: "Rezervisana",
    CLEANING: "Čišćenje",
    MAINTENANCE: "Održavanje",
    OUT_OF_ORDER: "Van funkcije"
  };

  // AUTHENTICATION
  $: auth.subscribe((value) => {
    isAuthenticated = value.isAuthenticated;
  });

  function checkDarkmode() {
    // Check whether dark mode class is assigned to body.
    const check = () =>
      (isDark = document.body.classList.contains("dark"));

    check();

    const observer = new MutationObserver(check);

    observer.observe(document.body, {
      attributes: true,
      attributeFilter: ["class"]
    });

    return () => observer.disconnect();
  }

  /**
   * Support hash-based routing:
   * Extract query parameters from the hash if present.
   */
  function checkListViewParam() {
    let search = window.location.search;

    if (!search && window.location.hash.includes("?")) {
      search = window.location.hash.substring(
        window.location.hash.indexOf("?")
      );
    }

    const params = new URLSearchParams(search);

    let listViewParam = params.get("listview");

    if (listViewParam !== null) {
      isListView = listViewParam === "true";
    }

    listViewParam = params.get("listView");

    if (listViewParam !== null) {
      isListView = listViewParam === "true";
    }
  }

  // Fetch rooms from the backend
  onMount(async () => {
    checkDarkmode();
    checkListViewParam();

    try {
      const { isAuthenticated } = get(auth);

      isAdminView = isAdmin();

      if (!isAuthenticated) {
        error = "Session expired. Please login again.";
        return;
      } else {
        handleSearch();
      }
    } catch (err) {
      error = err instanceof Error ? err.message : "Search failed";
    }
  });

  function handleFormSubmit(event: { preventDefault: () => void }) {
    event.preventDefault();

    page = 0;
    handleSearch();
  }

  async function handleSearch() {
    const token = get(auth).token;

    loading = true;
    error = null;

    try {
      const res = await fetch(
        `${API_BASE_URL}/rooms?page=${page}&size=${size}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        }
      );

      if (!res.ok) {
        if (res.status === 401) {
          localStorage.removeItem("token");

          auth.set({
            token: null,
            isAuthenticated: false
          });

          throw new Error("Authentication failed");
        }

        throw new Error(
          `Fetch error: ${res.status} - ${res.statusText}`
        );
      }

      const data = await res.json();

      /*
       * Expected paginated response:
       * {
       *   rooms: [...],
       *   totalCount: 10,
       *   totalPages: 1
       * }
       *
       * If your backend returns a plain array,
       * replace this mapping accordingly.
       */
      rooms = data.rooms ?? data.content ?? data;

      totalRooms = data.totalCount ?? data.totalElements ?? rooms.length;
      totalPages = data.totalPages ?? Math.ceil(totalRooms / size);

      // Client-side filtering by room number/type/status.
      if (searchTerm.trim()) {
        const term = searchTerm.toLowerCase().trim();

        rooms = rooms.filter((room) =>
          room.roomNumber?.toLowerCase().includes(term) ||
          room.type?.toLowerCase().includes(term) ||
          room.status?.toLowerCase().includes(term) ||
          getRoomTypeLabel(room.type).toLowerCase().includes(term)
        );
      }

    } catch (err: any) {
      console.error(err);

      if (err.message?.includes("401")) {
        auth.set({
          token: null,
          isAuthenticated: false
        });
      } else {
        error =
          err instanceof Error
            ? err.message
            : "Unknown error";
      }
    } finally {
      loading = false;
    }
  }

  function nextPage() {
    if ((page + 1) * size < totalRooms) {
      page += 1;
      handleSearch();
    }
  }

  function prevPage() {
    if (page > 0) {
      page -= 1;
      handleSearch();
    }
  }

  function toggleView() {
    isListView = !isListView;
  }

  function getRoomTypeLabel(type: string): string {
    return roomTypes[type] ?? type;
  }

  function getRoomStatusLabel(status: string): string {
    return roomStatuses[status] ?? status;
  }

  function getRoomStatusColor(status: string): string {
    switch (status) {
      case "AVAILABLE":
        return "success";
      case "OCCUPIED":
        return "error";
      case "RESERVED":
        return "warning";
      case "CLEANING":
        return "info";
      case "MAINTENANCE":
        return "warning";
      case "OUT_OF_ORDER":
        return "neutral";
      default:
        return "neutral";
    }
  }

  function formatRoomPrice(price: number | null | undefined): string {
    if (price === null || price === undefined) {
      return "-";
    }

    return new Intl.NumberFormat("hr-HR", {
      style: "currency",
      currency: "EUR",
      minimumFractionDigits: 2
    }).format(price);
  }

  function deleteRoom(id: number) {
    // TODO: Implement delete confirmation and API call.
    alert(`Brisanje sobe ${id} još nije implementirano.`);
  }
</script>

{#if !$auth.isAuthenticated}
  <Login />

{:else}
  {#if error}
    <ErrorDiv {error} />
  {:else}
    <div class="w-full flex justify-center px-4">
      <div class="w-full max-w-4xl p-4 bg-base-200 rounded-lg">

        <form
          on:submit|preventDefault={handleFormSubmit}
          class="flex flex-col gap-3"
        >
          <!-- Gornji red: Input i Traži dugme -->
          <div class="flex gap-2">
            <input
              type="text"
              bind:value={searchTerm}
              placeholder="Traži sobu..."
              class="input input-secondary border-2 flex-1"
            />

            <button type="submit" class="btn btn-dash">
              <i class="fas fa-search"></i>
              <span class="hidden sm:inline ml-1">Traži</span>
            </button>
          </div>

          <!-- Donji red: Dodaj, Pagination i View toggle -->
          <div class="flex gap-2 items-center">

            {#if getCurrentRole() === "ADMIN"}
              <a
                href="#/inventory/0"
                use:link
                class="btn btn-dash flex-1 lg:flex-none whitespace-nowrap"
              >
                <i class="fas fa-plus"></i>
                Dodaj novu
              </a>
            {/if}

            <div class="flex-1"></div>

            <div class="text-xs sm:text-sm text-gray-600 dark:text-gray-400 whitespace-nowrap">
              Strana <b>{page + 1}</b> od <b>{totalPages}</b>
              <span class="hidden md:inline">
                | Ukupno: <b>{totalRooms}</b>
              </span>
            </div>

            <button
              type="button"
              on:click={toggleView}
              class="btn btn-dash whitespace-nowrap"
            >
              <i class="fas fa-th-list"></i>
              <span class="hidden sm:inline ml-1">
                {#if isListView}
                  List
                {:else}
                  Grid
                {/if}
              </span>
            </button>
          </div>
        </form>

      </div>
    </div>

    {#if loading}
      <LoadingOverlay />
    {/if}

    {#if isListView && isAdminView}

      <!-- TABLE VIEW -->
      <div class="w-full max-w-[2048px] overflow-x-auto rounded-lg align-middle text-center mx-auto mt-8">

        <table class="table table-zebra min-w-full divide-y divide-accent">

          <thead class="bg-base-300">
            <tr class="h-12">
              <th class="pgs-th">Soba</th>
              <th class="pgs-th">Status</th>
              <th class="pgs-th">Tip</th>
              <th class="pgs-th">Sprat</th>
              <th class="pgs-th">Kapacitet</th>
              <th class="pgs-th-r">Cena / noć</th>
              <th class="pgs-th">Aktivna</th>
              <th class="pgs-th">Kreirano</th>
              <th class="pgs-th">Izmenjeno</th>
              <th class="pgs-th">Akcije</th>
            </tr>
          </thead>

          <tbody>
            {#each rooms as room, i}
              <tr
                class={`tr-highlight ${
                  i % 2 === 1
                    ? "bg-base-200/30"
                    : "bg-base-200/60"
                }`}
              >

              <!-- Room number -->
                <td class="pgs-td">
                  <a
                    use:link
                    href="#/inventory/{room.id}"
                    class="pgs-hyperlink font-bold"
                  >
                    {room.roomNumber}
                  </a>
                </td>

                <!-- Room status -->
                <td class="text-center">
                  <span
                    class={`badge badge-soft badge-${getRoomStatusColor(room.status)} font-mono badge-sm uppercase`}
                  >
                    {getRoomStatusLabel(room.status)}
                  </span>
                </td>                

                <!-- Room type -->
                <td class="pgs-td">
                  <span class="badge badge-soft badge-info font-mono">
                    {getRoomTypeLabel(room.type)}
                  </span>
                </td>

                <!-- Floor -->
                <td class="pgs-td font-mono">
                  {room.floor}.
                </td>

                <!-- Capacity -->
                <td class="pgs-td font-mono">
                  <i class="fas fa-users text-gray-400 mr-1"></i>
                  {room.capacity}
                </td>

                <!-- Price -->
                <td class="pgs-td-num font-mono font-bold text-right">
                  {formatRoomPrice(room.pricePerNight)}
                </td>

                <!-- Active -->
                <td class="text-center">
                  <span
                    class={`badge badge-soft ${
                      room.active ? "badge-success" : "badge-neutral"
                    } font-mono badge-sm`}
                  >
                    {room.active ? "🟢" : "⚫"}
                  </span>
                </td>

                <!-- Created -->
                <td class="pgs-td font-mono">
                  {@html formatDate(room.created, "new", 15)}
                </td>

                <!-- Modified -->
                <td class="pgs-td font-mono">
                  {@html formatDate(room.modified, "new", 15)}
                </td>

                <!-- Actions -->
                <td class="px-2">
                  <div class="flex justify-center items-center gap-2">

                    <div class="tooltip tooltip-info group" data-tip="Edit">
                      <a
                        class="px-4"
                        aria-label="Edit"
                        use:link
                        href="#/inventory/{room.id}"
                      >
                        <i class="fas fa-pen text-gray-500 group-hover:text-sky-400 cursor-pointer"></i>
                      </a>
                    </div>

                    {#if getCurrentRole() === "ADMIN"}
                      <button
                        class="px-4 group"
                        aria-label="Delete"
                        on:click={() => deleteRoom(room.id)}
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
          Prikazano:
          <span class="font-bold font-mono text-xl text-primary">
            {rooms.length}
          </span>
          soba
          <br />
          Ukupno u bazi:
          <span class="font-bold text-lg text-primary">
            {totalRooms}
          </span>
          | Stranica:
          <span class="font-bold text-lg text-primary">
            {totalPages}
          </span>
        </div>

      </div>

    {:else if rooms.length === 0 && !loading}

      <div class="text-center mt-16 text-gray-500">
        Nema pronađenih soba :/
      </div>

    {:else}

      <!-- GRID VIEW -->
      <div
        class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-8 p-4"
        style="justify-items: center;"
      >

        {#each rooms as room}
  <div class="card bg-base-200 shadow-xl w-full max-w-sm border border-primary/10 overflow-hidden">

    <!-- Room image -->
    {#if room.imageUrl}
      <figure class="h-48">
        <img
          src={room.imageUrl}
          alt={`Soba ${room.roomNumber}`}
          class="w-full h-full object-cover"
        />
      </figure>
    {:else}
      <figure class="h-48 bg-base-300 flex items-center justify-center">
        <i class="fas fa-bed text-5xl text-base-content/20"></i>
      </figure>
    {/if}

    <div class="card-body">

      <div class="flex justify-between items-start">
        <h2 class="card-title text-primary">
          {room.roomNumber}
        </h2>

        <span
          class={`badge badge-soft badge-${getRoomStatusColor(room.status)} font-mono badge-sm`}
        >
          {getRoomStatusLabel(room.status)}
        </span>
      </div>

      <div class="divider my-1"></div>

      <div class="space-y-3">

        <div class="flex justify-between">
          <span class="text-secondary">Tip</span>
          <span class="font-semibold">
            {getRoomTypeLabel(room.type)}
          </span>
        </div>

        <div class="flex justify-between">
          <span class="text-secondary">Sprat</span>
          <span class="font-mono">
            {room.floor}.
          </span>
        </div>

        <div class="flex justify-between">
          <span class="text-secondary">Kapacitet</span>
          <span class="font-mono">
            <i class="fas fa-users text-gray-400 mr-1"></i>
            {room.capacity}
          </span>
        </div>

        <div class="flex justify-between">
          <span class="text-secondary">Cena / noć</span>
          <span class="font-mono font-bold text-primary">
            {formatRoomPrice(room.pricePerNight)}
          </span>
        </div>

        <div class="flex justify-between">
          <span class="text-secondary">Aktivna</span>
          <span class={room.active ? "text-success" : "text-gray-500"}>
            {room.active ? "DA" : "NE"}
          </span>
        </div>

      </div>

      {#if room.description}
        <div class="mt-3 text-sm text-secondary">
          {room.description}
        </div>
      {/if}

      <div class="card-actions justify-end mt-4">

        <a
          href="#/inventory/{room.id}"
          use:link
          class="btn btn-sm btn-outline"
        >
          <i class="fas fa-pen"></i>
          Uredi
        </a>

        {#if getCurrentRole() === "ADMIN"}
          <button
            class="btn btn-sm btn-outline hover:btn-error"
            on:click={() => deleteRoom(room.id)}
          >
            <i class="fas fa-trash"></i>
          </button>
        {/if}

      </div>

    </div>

  </div>
{/each}

      </div>

    {/if}

  {/if}
{/if}

<style>
  .pgs-th {
    color: white;
  }
</style>