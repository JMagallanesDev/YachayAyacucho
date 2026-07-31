import nextCoreWebVitals from "eslint-config-next/core-web-vitals";
import nextTypeScript from "eslint-config-next/typescript";

/**
 * Configuracion de ESLint en formato flat config nativo.
 *
 * Desde Next.js 16, `eslint-config-next` publica sus configuraciones ya como
 * flat config y se importan directamente. La version anterior las envolvia
 * con el puente `FlatCompat` de `@eslint/eslintrc`, que con la 16 falla al
 * intentar serializar una estructura circular de plugins.
 */
const eslintConfig = [
  ...nextCoreWebVitals,
  ...nextTypeScript,
  {
    ignores: ["node_modules/**", ".next/**", "out/**", "build/**", "next-env.d.ts"],
  },
];

export default eslintConfig;
