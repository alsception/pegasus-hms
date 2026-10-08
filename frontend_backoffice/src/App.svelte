<script lang="ts">
  //Svelte imports
  import { get } from "svelte/store";
  import { onMount } from "svelte";
  import Router, { link, push } from "svelte-spa-router";

  //Our imports - core
  import { auth, getCurrentRole } from "./core/services/SessionStore";
  import Login from "./core/auth/Login.svelte";
  import Header from "./core/navigation/Header.svelte";
  import { generateRoutes } from "./core/navigation/routing/routes";
  import { SvelteToast } from "@zerodevx/svelte-toast";
  import InfoModal from "./core/utils/ErrorModal.svelte";
  import { navRoutes } from "./core/navigation/menu/navRoutes";
  import type { NavRoutesMap } from "./core/navigation/menu/MenuTypes";
  import { connectNotificationsWebsocket } from "./core/services/Notifications";
  import NotificationsInfo from "./core/navigation/NotificationsInfo.svelte";

  document.title = 'Pegasus'

  export const unauthenticatedRoutes = {
    '/login': Login,
    /*'/products': ProductsListBarbacoa, mozda jednog dana?*/
    '*': Login  // fallback route
  };

  //Definitions
  let isAuthenticated = false;
  let isDark = false;

  //Authenticacion
  $: auth.subscribe((value) => {
    isAuthenticated = value.isAuthenticated;
  });

  //TODO: start refactoring block, ovo bi trebalo pojednostaviti, previse je komplikovano i nepotrebno.

  const role = getCurrentRole();  

  //Filter nav items by role
  const getRoutesByRole = (role: 'ADMIN' | 'CUSTOMER') => {
    return Object.entries(navRoutes)
      .filter(([_, route]) => {
        if(route.default) return true;
        if (role === 'ADMIN') return route.admin === true;
        if (role === 'CUSTOMER') return route.customer === true;
        return false;
      })
      .reduce((acc, [path, route]) => {
        acc[path] = route;
        return acc;
      }, {} as NavRoutesMap);
  };

  const filteredRoutes = getRoutesByRole(role);

  // Function to get navigation items (for menu display)
  function getNavigationItems() {
    return Object.values(filteredRoutes).map((route) => ({
      label: route.label,
      icon: route.icon,
      href: route.href,
      inProgress: route.inProgress,
      tags: route.tags,
      disabled: route.disabled,
      hidden: !!route.hidden,
    }));
  }
  let navItems = getNavigationItems();

  //END REFACTORING BLOCK//

  onMount(async () => 
  {
    try 
    {
      const { isAuthenticated: authStatus } = get(auth);
      isAuthenticated = authStatus;

      if(isAuthenticated)
      {
        connectNotificationsWebsocket();
      }

      const params = new URLSearchParams(window.location.search);
      let page = params.get('page');
      if (page === 'completion') 
      {
        // Ovo treba za placanje karticom, taraba problem
        const clientSecret = params.get('payment_intent_client_secret');
        const query = window.location.search; // preserve Stripe params
        window.location.replace(`/#/completion${query}`);
      }
    } 
    catch (err) 
    {
       console.error(err);
    } 
  });

  const routes = generateRoutes();

</script>

{#if !$auth.isAuthenticated}
  <Router routes={unauthenticatedRoutes} />
{:else}

<!-- NAPOMENA: U HEADERU SE NALAZI KOMPONENTA ZA DARKMODE! 
 Za sad mora da bude ovako, inace se pokvare boje [TODO]-->
<div style="display: none;">
<Header/> 
</div>

<div class="drawer md:drawer-open">

  <input id="my-drawer" type="checkbox" class="drawer-toggle" />

  <div class="drawer-content">
    
    <!-- Navbar -->
    <nav class="navbar w-full bg-base-100 shadow-sm">

      <!-- Mobile / tablet menu button -->
      <label
        for="my-drawer"
        aria-label="open sidebar"
        class="btn btn-square btn-ghost drawer-button md:hidden"

      >
        <i class="fas fa-bars text-xl"></i>        
      </label>

      <div class="px-4 font-bold text-primary">
        PEGASUS HMS
      </div>
<div class="dropdown ml-auto">
      <button
        tabindex="0"
        class="btn btn-ghost p-0 hover:bg-primary/10 text-gray-500 text-xl"
        data-tip="Notifications"
        aria-label="Notifications"
      >
        <i id="notifications-icon" class="fas fa-bell text-sm md:text-lg"></i>
        <span
          id="notifications-indicator"
          class="text-sm text-zinc-50 bg-error px-1 hidden"
          style="position: relative;top: -0.5rem;left: -0.5rem;">12</span
        >
      </button>

      <NotificationsInfo />
    </div>
<!-- 
        <ul
          class="menu menu-sm dropdown-content w-52 p-2 scale-in-ver-top
                bg-base-100 dark:bg-zinc-900
                rounded shadow
                max-h-180 overflow-x-auto block"
          style="min-width: 300px; left: -270px; top: 44px"
        >
          <li class="flex  px-3 py-2 rounded-md">
            <div class="inline-flex gap-1">
            No new notifications
            </div>
          </li>
          <li class=""></li>
                <li class="w-full border-t border-primary/10">
                    <div class="flex items-center px-3 py-2 rounded-md cursor-pointer
                                hover:bg-base-200 hover:text-blue-400
                                text-primary text-sm"
                    >
                        <i class="fas fa-info w-5 mr-3"></i>

                        <div class="flex flex-col">
                        <p class="font-bold dark:text-gray-400">
asd                        </p>
                        <p class="text-xs dark:text-gray-500 ">
fgh                        </p>
                        <p class="text-xs dark:text-gray-600 ">fgh</p>
                        </div>
                    </div>
                </li>
        </ul>

 -->
    </nav>

    <!-- Main content goes here inside routes-->
    <main class="flex-1 overflow-auto main-content w-full p-0 sm:p-6">
      <Router {routes} />
    </main>

  </div>

  <!-- Sidebar -->
  <div class="drawer-side is-drawer-close:overflow-visible z-150 border-r border-primary/5">

    <label
      for="my-drawer"
      aria-label="close sidebar"
      class="drawer-overlay"
    ></label>

    <div
      class="flex min-h-full flex-col items-start bg-base-200
             is-drawer-close:w-14
             is-drawer-open:w-60"
    >

      <ul class="menu w-full grow">

        <!-- Sidebar toggle -->
        <label
          for="my-drawer"
          aria-label="toggle sidebar"
          class="btn btn-square btn-ghost drawer-button"
        >
          <i class="fas fa-bars text-xl light-icon"></i>
        </label>

        {#each navItems as item}
          <li class="py-1">
            <a
              use:link
              href={item.disabled ? "#" : item.href}
              class="is-drawer-close:tooltip is-drawer-close:tooltip-right
                    is-drawer-close:tooltip-primary
                    text-primary/80"
              data-tip={item.label}
            >
              <span class="w-6">
                <i class="fas fa-regular fa-{item.icon} light-icon"></i>
              </span>            
              
              <span class="is-drawer-close:hidden font-semibold">
                {item.label}
              </span>
            </a>
          </li>
        {/each}

      </ul>
    </div>
  </div>
</div>

{/if}

<InfoModal />
<SvelteToast />

<style>
  .light-icon{
    opacity: 0.8;
  }
</style>