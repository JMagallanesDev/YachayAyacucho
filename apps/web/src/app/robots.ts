import type { MetadataRoute } from "next";

import { env } from "@/lib/env";

/**
 * robots.txt (Bloque 13).
 *
 * <p>Se prohibe lo que no tiene nada que indexar y ademas no deberia acabar en
 * una cache publica: el panel de administracion, el area privada del visitante
 * y las rutas internas del propio Next.</p>
 *
 * <p><strong>Esto no es una medida de seguridad.</strong> Un robots.txt es una
 * peticion educada que cualquier cliente puede ignorar, y de hecho publica la
 * lista de rutas que uno preferiria que no se miraran. Lo que protege
 * {@code /admin} es la regla {@code hasRole('ADMIN')} del backend y el
 * {@code proxy.ts}; esto solo evita que un buscador pierda el tiempo y muestre
 * en resultados una pagina que devolveria un 403.</p>
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/api/", "/_next/", "/es/admin", "/en/admin", "/es/perfil", "/en/perfil"],
    },
    sitemap: `${env.siteUrl}/sitemap.xml`,
    host: env.siteUrl,
  };
}
