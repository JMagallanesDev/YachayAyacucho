import type { Metadata, Viewport } from "next";
import { hasLocale, NextIntlClientProvider } from "next-intl";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Inter, Playfair_Display } from "next/font/google";
import { notFound } from "next/navigation";
import Script from "next/script";
import type { ReactNode } from "react";

import { ProveedorQuery } from "@/components/ProveedorQuery";
import { ProveedorSesion } from "@/components/ProveedorSesion";
import { SelectorIdioma } from "@/components/SelectorIdioma";
import { NavegacionPrincipal } from "@/components/navegacion/NavegacionPrincipal";
import { GUION_ANTI_DESTELLO } from "@/components/tema/tema";
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

  const otroIdioma = locale === "es" ? "en" : "es";

  return {
    /*
     * `metadataBase` es obligatorio para que Open Graph funcione: sin el, Next
     * emite las imagenes con rutas relativas y ninguna red social las resuelve
     * —el enlace compartido sale sin miniatura—.
     */
    metadataBase: new URL(env.siteUrl),
    title: { default: env.appName, template: `%s | ${env.appName}` },
    description: t("descripcion"),

    alternates: {
      canonical: `/${locale}`,
      languages: {
        [locale]: `/${locale}`,
        [otroIdioma]: `/${otroIdioma}`,
      },
    },

    openGraph: {
      type: "website",
      siteName: env.appName,
      title: env.appName,
      description: t("descripcion"),
      locale: locale === "es" ? "es_PE" : "en_US",
      url: `/${locale}`,
    },

    twitter: {
      card: "summary_large_image",
      title: env.appName,
      description: t("descripcion"),
    },

    // El sitio es de difusion patrimonial: se quiere indexado y con vista
    // previa completa en los resultados.
    robots: {
      index: true,
      follow: true,
      googleBot: { index: true, follow: true, "max-image-preview": "large" },
    },
  };
}

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Imprescindible para que env(safe-area-inset-*) tenga efecto: sin esto el
  // contenido no llega bajo el notch y las safe areas valen siempre cero.
  viewportFit: "cover",
  // Los dos unicos colores que NO pueden salir de los tokens: el navegador
  // lee esta cabecera antes de que exista ninguna hoja de estilo, asi que no
  // hay CSS que consultar. Son el valor resuelto de --color-sillar-50 y
  // --color-piedra-950; si la paleta cambia, cambian aqui (RF-89).
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "oklch(0.970 0.027 65.7)" },
    { media: "(prefers-color-scheme: dark)", color: "oklch(0.190 0.005 65.7)" },
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
      suppressHydrationWarning
    >
      <body>
        {/*
          `next/script` con `beforeInteractive` y NO un <script> escrito a mano:
          React 19 avisa —con razon— de que «los scripts dentro de componentes
          nunca se ejecutan al renderizar en el cliente», porque al hidratar
          trata la etiqueta como un nodo mas y no la evalua. `next/script` la
          inyecta en el HTML inicial, fuera del arbol que React hidrata.

          Su unico trabajo es poner data-theme ANTES del primer pintado: sin
          el, quien tiene el modo oscuro veria un fogonazo blanco en cada
          carga. Ver tema.ts.
        */}
        <Script id="tema-anti-destello" strategy="beforeInteractive">
          {GUION_ANTI_DESTELLO}
        </Script>
        <NextIntlClientProvider>
          {/* Cache de estado de servidor compartida por toda la app: sin ella
              cada componente cliente refetchearia por su cuenta. */}
          <ProveedorQuery>
            {/* Intenta recuperar la sesion con la cookie httpOnly al cargar.
                El access token vive en memoria, asi que cada recarga lo pierde. */}
            <ProveedorSesion>
              {/*
                Primer elemento tabulable de la pagina (WCAG 2.4.1). Sin el,
                quien navega con teclado tiene que recorrer los cinco destinos
                de la barra en CADA pagina antes de llegar al contenido.
              */}
              <a href="#contenido" className="saltar-al-contenido press rounded-card bg-primary px-4 py-2 text-fluid-sm font-medium text-primary-fg">
                {(await getTranslations({ locale, namespace: "navegacion" }))("saltarAlContenido")}
              </a>

              <NavegacionPrincipal />
              {/* El hueco de la barra fija: abajo en movil, arriba en
                  escritorio. Sin esto la barra taparia la ultima fila de
                  cada pagina, que es el fallo clasico de una barra fija. */}
              <div
                id="contenido"
                className="pb-[calc(var(--spacing-nav)+env(safe-area-inset-bottom))] md:pt-nav md:pb-0"
              >
                {children}
              </div>
              <SelectorIdioma />
            </ProveedorSesion>
          </ProveedorQuery>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
