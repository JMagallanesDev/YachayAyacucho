import type { Metadata, Viewport } from "next";
import { hasLocale, NextIntlClientProvider } from "next-intl";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Inter, Playfair_Display } from "next/font/google";
import { notFound } from "next/navigation";
import type { ReactNode } from "react";

import { ProveedorQuery } from "@/components/ProveedorQuery";
import { ProveedorSesion } from "@/components/ProveedorSesion";
import { SelectorIdioma } from "@/components/SelectorIdioma";
import { routing } from "@/i18n/routing";
import { env } from "@/lib/env";

/**
 * Tipografia dual (RF-91): Playfair Display para titulos, con el aire
 * editorial que pide el contenido patrimonial, e Inter para el resto.
 * `display: swap` evita el texto invisible mientras carga la fuente, una de
 * las causas tipicas de mal CLS (RNF-24).
 */
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });
const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
  display: "swap",
});

/** Genera las dos versiones de idioma en compilacion, no bajo demanda. */
export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "portada" });

  return {
    title: { default: env.appName, template: `%s | ${env.appName}` },
    description: t("descripcion"),
  };
}

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Imprescindible para que env(safe-area-inset-*) tenga efecto: sin esto el
  // contenido no llega bajo el notch y las safe areas valen siempre cero.
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
 * dentro de la pagina. Hasta Next 15, Next anulaba ese valor durante los
 * cambios de ruta para que la navegacion saltara arriba al instante; en Next
 * 16 ya no lo hace por defecto, y sin este atributo cada navegacion haria un
 * scroll animado hasta el inicio, justo lo contrario de la sensacion de app
 * nativa. Este atributo restaura el comportamiento.
 */
export default async function LayoutIdioma({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;

  // Un idioma que no soportamos es un 404, no un fallback silencioso: la URL
  // /fr/perfil no existe, y fingir que si confundiria a buscadores y usuarios.
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }

  // Habilita el renderizado estatico de esta rama del arbol.
  setRequestLocale(locale);

  return (
    <html
      lang={locale}
      className={`${inter.variable} ${playfair.variable}`}
      data-scroll-behavior="smooth"
    >
      <body>
        <NextIntlClientProvider>
          {/* Cache de estado de servidor compartida por toda la app: sin ella
              cada componente cliente refetchearia por su cuenta. */}
          <ProveedorQuery>
            {/* Intenta recuperar la sesion con la cookie httpOnly al cargar.
                El access token vive en memoria, asi que cada recarga lo pierde. */}
            <ProveedorSesion>
              <SelectorIdioma />
              {children}
            </ProveedorSesion>
          </ProveedorQuery>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
