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

<div class="drawer lg:drawer-open">
  <input id="my-drawer-4" type="checkbox" class="drawer-toggle inline" />
  <div class="drawer-content">
    <!-- Navbar -->
    <nav class="navbar w-full bg-base-300">
      <label for="my-drawer-4" aria-label="open sidebar" class="btn btn-square btn-ghost drawer-button">
        <!-- Sidebar toggle icon -->
        <i class="fas fa-bars text-xl"></i>
      </label>
      <div class="px-4 font-bold">PEGASUS HMS</div>
    </nav>
    <!-- Page content here -->
    <main class="flex-1 overflow-auto main-content w-full p-0 sm:p-6">
      <Router {routes} />  
    </main>
  </div>

  <div class="drawer-side is-drawer-close:overflow-visible">
    <label for="my-drawer-4" aria-label="close sidebar" class="drawer-overlay"></label>
    <div class="flex min-h-full flex-col items-start bg-base-200 is-drawer-close:w-14 is-drawer-open:w-64">
      <!-- Sidebar content here -->
      <ul class="menu w-full grow">

        <!-- List items -->
        {#each navItems as item}
        <li>
          <a
            use:link
            href={item.disabled ? "#" : item.href}
            class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="{item.label}">
            <!-- icon -->
            <i class="fas fa-{item.icon} w-5"></i>
            <span class="is-drawer-close:hidden">{item.label}</span>
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