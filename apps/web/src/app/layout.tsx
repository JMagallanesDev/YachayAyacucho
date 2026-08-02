import type { Metadata, Viewport } from "next";
import { Inter, Playfair_Display } from "next/font/google";

import { ProveedorSesion } from "@/components/ProveedorSesion";
import { env } from "@/lib/env";
import "./globals.css";

/**
 * Tipografia dual (RF-91): Playfair Display para titulos, con el aire
 * editorial que pide el contenido patrimonial, e Inter para el resto.
 * `display: swap` evita el texto invisible mientras carga la fuente,
 * una de las causas tipicas de mal CLS (RNF-24).
 */
const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: env.appName,
    template: `%s | ${env.appName}`,
  },
  description:
    "Patrimonio cultural de Huamanga, Ayacucho: lugares, historia, rutas y agenda cultural.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Imprescindible para que env(safe-area-inset-*) tenga efecto: sin
  // esto el contenido no llega bajo el notch y las safe areas valen
  // siempre cero.
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f7f0e8" },
    { media: "(prefers-color-scheme: dark)", color: "#1a1715" },
  ],
};

/**
 * `data-scroll-behavior="smooth"` es necesario desde Next.js 16.
 *
 * `globals.css` declara `scroll-behavior: smooth` para el desplazamiento
 * dentro de la página. Hasta Next 15, Next anulaba ese valor durante los
 * cambios de ruta para que la navegación saltara arriba al instante; en
 * Next 16 ya no lo hace por defecto, y sin este atributo cada navegación
 * haría un scroll animado hasta el inicio, justo lo contrario de la
 * sensación de app nativa. Este atributo restaura el comportamiento.
 */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang={env.defaultLocale}
      className={`${inter.variable} ${playfair.variable}`}
      data-scroll-behavior="smooth"
    >
      <body>
        {/* Intenta recuperar la sesion con la cookie httpOnly al cargar.
            El access token vive en memoria, asi que cada recarga lo pierde. */}
        <ProveedorSesion>{children}</ProveedorSesion>
      </body>
    </html>
  );
}
