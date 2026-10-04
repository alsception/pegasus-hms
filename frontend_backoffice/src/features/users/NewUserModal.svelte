<script lang="ts">
  import { createEventDispatcher, onMount } from "svelte";
  import { fade } from "svelte/transition";
  import type { FPGSUser } from "./FPGSUser";
  import { toast } from "@zerodevx/svelte-toast";
  import api from "../../core/services/client";
  import { showErrorModal } from "../../utils/modal";

  export let isOpen = false;

  const dispatch = createEventDispatcher();

  let newUser: FPGSUser;

  function resetUser() {
    newUser = {
      id: 0,
      role: "",
      username: "",
      firstName: "",
      lastName: "",
      active: true,
      created: null,
      modified: null,
      password: null,
      email: null,
      phone: null,
    };
  }

  // Available user types
  const userTypes = [
  "ADMIN",
  "CUSTOMER",
    "WAITER",
    "KITCHEN",
    "EMPLOYEE",
    "TESTER",
    "OTHER",
  ]

  function closeModal() {
    isOpen = false;
    dispatch("close");
  }

  async function createUser(user: FPGSUser) {
    return api<FPGSUser>("/users", {
      method: "POST",
      body: JSON.stringify(user),
    });
  }

  function submitForm() 
  {
    submit(newUser);
    closeModal();
    resetUser();
  }

  async function submit(newUser: FPGSUser) 
  {
    try 
    {
      await createUser(newUser);
      //We just assume its created if no error happened
      toast.push('✅ User created');
      dispatch('close');//Ovo dispacuje event pa parent da zna da pokrene search
    } 
    catch (error: any)
    {
      console.error("Error creating user:", error);
      showErrorModal(error.message)
    }
  }

  onMount(() => {
    resetUser();
  });
</script>

<!-- Modal backdrop -->
{#if isOpen}
<div
  class="fixed inset-0 bg-black/50 z-40 flex items-center justify-center p-2 sm:p-4 glassdrop"
  on:keydown|self={(e) =>
    (e.key === "Escape" || e.key === "Enter") && closeModal()}
  tabindex="0"
  role="button"
  aria-label="Close modal"
  id="m-backdrop"
  transition:fade={{ duration: 200 }}
>
  <div
    class="bg-base-100 rounded-xl shadow-2xl max-w-2xl w-full z-50 overflow-hidden max-h-[95dvh] flex flex-col"
    transition:fade={{ duration: 200 }}
  >
    <!-- Header -->
    <div
      class="flex justify-between items-center px-4 py-2.5 sm:px-5 sm:py-4 border-b border-base-300 shrink-0"
    >
      <h2 class="text-base sm:text-xl font-semibold text-primary">
        <i class="fas fa-user-plus mr-2 text-secondary"></i>
        Novi korisnik
      </h2>

      <button
        type="button"
        class="btn btn-xs sm:btn-sm btn-ghost btn-circle text-base-content/60 hover:text-primary"
        aria-label="Close modal"
        on:click={closeModal}
      >
        <i class="fas fa-times"></i>
      </button>
    </div>

    <form
      class="flex flex-col flex-1 min-h-0"
      on:submit|preventDefault={submitForm}
    >
      <!-- Skrolabilni dio -->
      <div class="flex-1 min-h-0 overflow-y-auto bg-base-200 p-3 px-8 sm:p-5">
        <fieldset class="min-w-0 border-0 p-0 m-0">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-3 gap-y-2 sm:gap-x-5 sm:gap-y-4 sm:p-2">

            <!-- Username -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="username">
                <span>
                  <i class="fas fa-user mr-1 text-secondary text-xs sm:text-sm"></i>
                  Username
                  <span class="text-error ml-1">*</span>
                </span>
              </label>
              <input
                id="username"
                type="text"
                class="pgs-input w-full text-sm"
                placeholder="username"
                bind:value={newUser.username}
                required
              />
            </div>

            <!-- Password -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="password">
                <span>
                  <i class="fas fa-lock mr-1 text-secondary text-xs sm:text-sm"></i>
                  Password
                  <span class="text-error ml-1">*</span>
                </span>
              </label>
              <input
                id="password"
                type="password"
                class="pgs-input w-full text-sm"
                placeholder="password"
                bind:value={newUser.password}
                required
              />
            </div>

            <!-- First name -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="firstName">
                Ime
              </label>
              <input
                id="firstName"
                type="text"
                class="pgs-input w-full text-sm"
                placeholder="First name"
                bind:value={newUser.firstName}
              />
            </div>

            <!-- Last name -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="lastName">
                Prezime
              </label>
              <input
                id="lastName"
                type="text"
                class="pgs-input w-full text-sm"
                placeholder="Last name"
                bind:value={newUser.lastName}
              />
            </div>

            <!-- Email -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="email">
                <i class="fas fa-at mr-1 text-secondary text-xs sm:text-sm"></i>
                E-mail
              </label>
              <input
                id="email"
                type="email"
                class="pgs-input w-full text-sm"
                placeholder="email"
                bind:value={newUser.email}
              />
            </div>

            <!-- Phone -->
            <div class="min-w-0">
              <label class="label py-0.5 text-md sm:py-1" for="phone">
                <i class="fas fa-phone mr-1 text-secondary text-xs sm:text-sm"></i>
                Phone
              </label>
              <input
                id="phone"
                type="text"
                class="pgs-input w-full text-sm"
                placeholder="phone"
                bind:value={newUser.phone}
              />
            </div>

            <!-- Role -->
            <div class="min-w-0">
              <label for="role" class="label py-0.5 text-md sm:py-1">
                <span>
                  <i class="fas fa-user-tag mr-1 text-secondary text-xs sm:text-sm"></i>
                  Role
                  <span class="text-error ml-1">*</span>
                </span>
              </label>
              <select
                id="role"
                bind:value={newUser.role}
                class="pgs-input w-full text-sm"
                required
              >
                {#each userTypes as type}
                  <option value={type}>{type}</option>
                {/each}
              </select>
            </div>

            <!-- Active -->
            <div class="flex items-end pb-1 space-x-2 sm:space-x-3">
              <input
                type="checkbox"
                bind:checked={newUser.active}
                class="toggle toggle-sm sm:toggle-md ring-2 ring-primary bg-base-100 text-red-600 checked:text-green-600"
              />
              <p class="font-mono font-bold text-primary/80">
                {#if newUser.active}
                  <span class="text-green-600 text-sm sm:text-lg">AKTIVAN</span>
                {:else}
                  <span class="text-gray-600 text-sm sm:text-lg">NEAKTIVAN</span>
                {/if}
              </p>
            </div>

          </div>
        </fieldset>
      </div>

      <!-- Footer -->
      <div
        class="flex justify-end gap-2 sm:gap-3 px-4 py-2.5 sm:px-5 sm:py-4 border-t border-base-300 bg-base-100 shrink-0"
      >
        <button
          type="button"
          on:click={closeModal}
          class="btn btn-sm sm:btn-md btn-ghost"
        >
          Cancel
        </button>

        <button type="submit" class="btn btn-sm sm:btn-md btn-primary">
          <i class="fas fa-save"></i>
          Save
        </button>
      </div>
    </form>
  </div>
</div>
{/if}

<style>
  .bg-black {
    background-color: #00000070;
  }
</style>
