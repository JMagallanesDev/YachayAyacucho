import { ImageResponse } from "next/og";
import { getTranslations } from "next-intl/server";

import { env } from "@/lib/env";

/**
 * Imagen de Open Graph por defecto (Bloque 13).
 *
 * <p>Se <strong>genera</strong> en vez de guardarse como archivo: asi el
 * proyecto no arrastra un PNG que hay que reexportar cada vez que cambie el
 * nombre o la paleta, y la tarjeta sale en el idioma de la pagina.</p>
 *
 * <p>Es el respaldo para todo lo que no tiene foto propia. Sin ella, un enlace
 * compartido en WhatsApp o Facebook aparece como una linea de texto gris —
 * justamente el sitio donde mas se comparte contenido turistico—. Las fichas de
 * lugar con fotografia aprobada siguen usando la suya, que es mejor.</p>
 *
 * <p>Los colores van escritos aqui y no como tokens porque esto se dibuja en un
 * servidor sin CSS: son los valores sRGB de la paleta Ayacucho, los mismos que
 * produce el conversor de {@code lib/tokens.ts}.</p>
 */
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Imagen({ params }: { params: { locale: string } }) {
  const t = await getTranslations({ locale: params.locale, namespace: "portada" });

  const SILLAR = "#fbf3ea";
  const ANIL = "#24406e";
  const RETABLO = "#b3202b";
  const QUINUA = "#c0703a";
  const PIEDRA = "#3f3a35";

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: SILLAR,
          padding: 72,
          fontFamily: "sans-serif",
        }}
      >
        {/* Tres bandas de color: el retablo ayacuchano reducido a su esencia. */}
        <div style={{ display: "flex", gap: 12 }}>
          <div style={{ width: 120, height: 14, background: RETABLO, borderRadius: 7 }} />
          <div style={{ width: 60, height: 14, background: ANIL, borderRadius: 7 }} />
          <div style={{ width: 40, height: 14, background: QUINUA, borderRadius: 7 }} />
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          <div style={{ fontSize: 84, fontWeight: 700, color: ANIL, lineHeight: 1.05 }}>
            {env.appName}
          </div>
          <div style={{ fontSize: 34, color: PIEDRA, maxWidth: 900, lineHeight: 1.35 }}>
            {t("descripcion")}
          </div>
        </div>

        <div style={{ fontSize: 26, color: QUINUA, letterSpacing: 2, textTransform: "uppercase" }}>
          Huamanga · Ayacucho · Peru
        </div>
      </div>
    ),
    size,
  );
}
