import type { NavRoutesMap } from "./MenuTypes";

/***
 * This object is used for generating main menu
 */

export const navRoutes: NavRoutesMap = 
{
    "/home": {
      label: "Početak",
      icon: "house",
      href: "#/home",
      component: "/home",
      admin: true,
      customer: false,
      default: false,
    },
    "/users": {
      label: "Korisnici",
      icon: "users",
      href: "#/users",
      component: "/users",
      componentDetails: "/users/:id",
      admin: true,
      customer: false,
    },
    "/inventory": {
      label: "Sobe",
      icon: "bed",
      href: "#/inventory",
      component: "/inventory",
      componentDetails: "/inventory/:id",
      admin: true,
      customer: false,
    },
    "/reservations": {
      label: "Rezervacije",
      icon: "calendar",
      href: "#/reservations",
      component: null,
      disabled: false,
      admin: true,
      customer: true,
    },
    "/calendar": {
      label: "Kalendar",
      icon: "calendar-days",
      href: "#/calendar",
      component: null,
      disabled: false,
      admin: true,
      customer: false,
    },
    "/stats": {
      label: "Statistike",
      icon: "bar-chart",
      href: "#/stats",
      component: "/stats",
      admin: true,
      customer: false,
      default: false,
    },
    "/pix": {
      label: "Galerija",
      icon: "image",
      href: "#/pix",
      component: null,
      disabled: false,
      hidden: true,
      admin: false,
      customer: false,
    },  
    "/logout": {
      label: "Odjavi se ",
      icon: "right-from-bracket",
      href: "#/logout",
      component: "Logout",
      disabled: false,
      hidden: false,
      admin: true,
      customer: false,
      default: true,
    },
  };