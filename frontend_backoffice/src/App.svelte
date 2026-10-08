<script lang="ts">
  //Svelte imports
  import { get } from "svelte/store";
  import { onMount } from "svelte";
  import Router, { link, location, push } from "svelte-spa-router";

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

  $: isActive = (href: string) =>
  {      
    //Koja je ruta aktivna, njoj dodajemo css color da ga istaknemo
    let isActive = '#' + $location === href;
    return isActive;
  }
 
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
        <a
          use:link
          href=#/home
          class="text-primary/80 hover:text-info"
          data-tip="PEGASUS HMS"
        >PEGASUS HMS</a>       
        
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
          <li class="py-1" class:active={isActive(item.href)}>
            <a
              use:link
              href={item.disabled ? "#" : item.href}
              class="is-drawer-close:tooltip is-drawer-close:tooltip-right
                    is-drawer-close:tooltip-primary
                    text-primary/70"
              data-tip={item.label}
              style="border-radius: initial;"
            >
              <span class="w-6">
                <i class="fas fa-regular fa-{item.icon} light-icon"></i>
              </span>            
              
              <span class="is-drawer-close:hidden">
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

  .active{
    background-color: color-mix(in oklab, var(--color-info) 10%, transparent);
    border: 1pt solid color-mix(in oklab, var(--color-info) 10%, transparent);
  }
  .active > a
  {
    color: var(--color-info);
    font-weight: bold;
  }
</style>