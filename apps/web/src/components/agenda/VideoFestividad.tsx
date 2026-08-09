"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

/**
 * Video de una festividad, con carga diferida por fachada (RF-12).
 *
 * <p><strong>El iframe no existe hasta que alguien pulsa play.</strong> Un embed
 * normal de YouTube descarga alrededor de un megabyte y planta cookies de
 * seguimiento a <em>todo</em> el que abra la pagina, haya querido ver el video o
 * no. Aqui lo que se pinta primero es la miniatura —una imagen— y el reproductor
 * se inserta con el primer clic, que es tambien el momento en que la persona
 * consiente.</p>
 *
 * <p>El dominio es {@code youtube-nocookie.com}, que no escribe cookies de
 * publicidad hasta la reproduccion.</p>
 *
 * <p>El componente recibe el <strong>identificador</strong>, no una URL: el
 * backend guarda solo eso y valida su formato con un CHECK, de modo que aqui no
 * puede llegar una direccion arbitraria para meter en un iframe.</p>
 */
export function VideoFestividad({ videoId, titulo }: { videoId: string; titulo: string }) {
  const t = useTranslations("agenda");
  const [reproduciendo, setReproduciendo] = useState(false);

  // Ultima linea de defensa: aunque el backend ya lo valida, este componente no
  // compone ninguna URL con algo que no sea un identificador legitimo.
  if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) {
    return null;
  }

  if (reproduciendo) {
    return (
      <div className="aspect-video w-full overflow-hidden rounded-card" data-testid="video-activo">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0`}
          title={titulo}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
          referrerPolicy="strict-origin-when-cross-origin"
          className="size-full border-0"
        />
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={() => setReproduciendo(true)}
      data-testid="fachada-video"
      aria-label={t("reproducirVideo", { titulo })}
      className="press relative aspect-video w-full overflow-hidden rounded-card"
    >
      {/* La miniatura la sirve YouTube como imagen estatica: sin scripts y sin
          cookies. `hqdefault` existe siempre; las de mayor resolucion no. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={`https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`}
        alt=""
        className="size-full object-cover"
      />
      <span className="absolute inset-0 flex items-center justify-center bg-black/30">
        <span className="flex size-16 items-center justify-center rounded-full bg-white/90 text-2xl text-anil-800">
          ▶
        </span>
      </span>
    </button>
  );
}
