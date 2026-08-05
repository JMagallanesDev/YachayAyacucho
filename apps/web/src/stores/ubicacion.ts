import { create } from "zustand";

/**
 * Ubicacion del visitante, para calcular distancias a pie (RF-09c).
 *
 * <p><strong>Sin `persist`, igual que la sesion.</strong> Una posicion
 * guardada de una visita anterior seria mentira en cuanto el usuario se
 * mueva, y mostrar "a 3 minutos" cuando esta a diez kilometros es peor que no
 * mostrar nada.</p>
 */

export type EstadoPermiso = "desconocido" | "disponible" | "concedido" | "denegado" | "pidiendo";

interface EstadoUbicacion {
  lat: number | null;
  lon: number | null;
  permiso: EstadoPermiso;
  /** Consulta el permiso sin provocar el dialogo del navegador. */
  comprobarPermiso: () => Promise<void>;
  /** Pide la posicion. Solo debe llamarse tras un gesto del usuario. */
  solicitar: () => void;
  descartar: () => void;
}

export const useUbicacion = create<EstadoUbicacion>((set, get) => ({
  lat: null,
  lon: null,
  permiso: "desconocido",

  /**
   * La Permissions API dice en que estado esta el permiso **sin pedirlo**.
   *
   * <p>Es lo que permite no ofrecer un boton que no haria nada: si el usuario
   * ya denegó, el navegador no volvera a preguntar, y mostrar el boton solo
   * produce frustracion. Si ya concedió, se usa en silencio.</p>
   *
   * <p>Safari no la implementa para geolocalizacion; en ese caso se deja en
   * "disponible" y el boton se muestra, que es el comportamiento correcto
   * cuando no se sabe.</p>
   */
  comprobarPermiso: async () => {
    if (typeof navigator === "undefined" || !("geolocation" in navigator)) {
      set({ permiso: "denegado" });
      return;
    }

    if (!navigator.permissions?.query) {
      set({ permiso: "disponible" });
      return;
    }

    try {
      const estado = await navigator.permissions.query({ name: "geolocation" });
      if (estado.state === "granted") {
        set({ permiso: "concedido" });
        get().solicitar();
      } else {
        set({ permiso: estado.state === "denied" ? "denegado" : "disponible" });
      }
    } catch {
      set({ permiso: "disponible" });
    }
  },

  solicitar: () => {
    if (typeof navigator === "undefined" || !("geolocation" in navigator)) {
      set({ permiso: "denegado" });
      return;
    }

    set({ permiso: "pidiendo" });
    navigator.geolocation.getCurrentPosition(
      (posicion) =>
        set({
          lat: posicion.coords.latitude,
          lon: posicion.coords.longitude,
          permiso: "concedido",
        }),
      // Denegar no rompe nada: las tarjetas siguen sin distancia.
      () => set({ permiso: "denegado", lat: null, lon: null }),
      {
        // Precision moderada: para "a 12 minutos caminando" no hace falta
        // encender el GPS de alta precision y gastar bateria.
        enableHighAccuracy: false,
        timeout: 10_000,
        maximumAge: 5 * 60 * 1000,
      },
    );
  },

  descartar: () => set({ lat: null, lon: null, permiso: "disponible" }),
}));
