"use client";

import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from "chart.js";
import { Bar, Line } from "react-chartjs-2";
import { useEffect, useState } from "react";

/**
 * Graficos del panel (RF-52).
 *
 * <p><strong>Decisiones de lectura, no de gusto.</strong></p>
 *
 * <ul>
 *   <li><strong>Un solo color por grafico.</strong> Cada uno pinta una sola
 *       serie, asi que no hay identidades que distinguir y una paleta de
 *       colores solo seria ruido. En el reparto por categoria fue ademas una
 *       decision forzada por los datos: el catalogo repite colores —tres
 *       categorias comparten el mismo verde—, de modo que colorear por
 *       categoria sugeriria una agrupacion que no existe. El nombre de cada
 *       barra lleva la identidad; la longitud lleva el dato.</li>
 *   <li><strong>Barras horizontales y no un grafico de tarta</strong> para los
 *       repartos: comparar longitudes desde una linea base comun es facil;
 *       comparar angulos no lo es.</li>
 *   <li><strong>Sin leyenda</strong>, porque el titulo de cada tarjeta ya dice
 *       que se esta mirando.</li>
 *   <li><strong>Rejilla discreta y ejes tenues</strong>: lo que tiene que
 *       destacar son los datos.</li>
 * </ul>
 *
 * <p>El color de la serie sale de los tokens de la marca y esta comprobado
 * contra las dos superficies: {@code quinua-600} sobre fondo claro y
 * {@code quinua-500} sobre oscuro, ambos con contraste suficiente y sin caer en
 * la banda en la que un color empieza a leerse como gris.</p>
 */

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler,
  Tooltip,
);

/** Lee un token del tema actual; funciona igual en claro y en oscuro. */
function token(nombre: string, respaldo: string): string {
  if (typeof window === "undefined") {
    return respaldo;
  }
  const valor = getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
  return valor || respaldo;
}

/**
 * Colores del tema, releidos cuando cambia el esquema del sistema.
 *
 * <p>Chart.js dibuja sobre un lienzo: no hereda CSS ni reacciona a un cambio de
 * tema por su cuenta. Hay que darle valores concretos y volver a dibujarlo.</p>
 */
function useColoresDelTema() {
  const [colores, setColores] = useState({
    serie: "#944e1b",
    texto: "#57534e",
    rejilla: "rgba(120,113,108,0.18)",
  });

  useEffect(() => {
    const consulta = window.matchMedia("(prefers-color-scheme: dark)");

    function leer() {
      const oscuro =
        document.documentElement.dataset.theme === "dark" ||
        (!document.documentElement.dataset.theme && consulta.matches);

      setColores({
        // El paso cambia con el fondo a proposito: el mismo color sobre una
        // superficie oscura se hunde y deja de distinguirse.
        serie: oscuro
          ? token("--color-quinua-500", "#c0703a")
          : token("--color-quinua-600", "#944e1b"),
        texto: token("--color-text-muted", oscuro ? "#a8a29e" : "#57534e"),
        rejilla: oscuro ? "rgba(214,211,209,0.14)" : "rgba(120,113,108,0.18)",
      });
    }

    leer();
    consulta.addEventListener("change", leer);
    return () => consulta.removeEventListener("change", leer);
  }, []);

  return colores;
}

const SIN_ANIMACION_SI_MOLESTA =
  typeof window !== "undefined" &&
  window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;

export interface PuntoDiario {
  fecha: string;
  valor: number;
}

export interface Reparto {
  etiqueta: string;
  valor: number;
}

/** Serie temporal: un area suave con la linea encima. */
export function GraficoSerie({
  puntos,
  etiquetaSerie,
  comoBarras = false,
}: {
  puntos: PuntoDiario[];
  etiquetaSerie: string;
  comoBarras?: boolean;
}) {
  const colores = useColoresDelTema();

  // Solo se etiqueta uno de cada cinco dias: con treinta fechas encima del eje
  // no se lee ninguna.
  const etiquetas = puntos.map((punto, i) =>
    i % 5 === 0 || i === puntos.length - 1 ? punto.fecha.slice(5) : "",
  );

  const datos = {
    labels: etiquetas,
    datasets: [
      {
        label: etiquetaSerie,
        data: puntos.map((punto) => punto.valor),
        borderColor: colores.serie,
        backgroundColor: comoBarras ? colores.serie : `${colores.serie}22`,
        borderWidth: 2,
        borderRadius: comoBarras ? 4 : undefined,
        fill: !comoBarras,
        tension: 0.3,
        pointRadius: 0,
        pointHoverRadius: 5,
      },
    ],
  };

  const opciones = {
    responsive: true,
    maintainAspectRatio: false,
    animation: SIN_ANIMACION_SI_MOLESTA ? (false as const) : undefined,
    interaction: { mode: "index" as const, intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        // El eje muestra la fecha recortada; el tooltip da la completa.
        callbacks: {
          title: (elementos: { dataIndex: number }[]) =>
            puntos[elementos[0]?.dataIndex]?.fecha ?? "",
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: colores.texto, maxRotation: 0, autoSkip: false },
        border: { color: colores.rejilla },
      },
      y: {
        beginAtZero: true,
        grid: { color: colores.rejilla },
        ticks: { color: colores.texto, precision: 0 },
        border: { display: false },
      },
    },
  };

  return (
    <div className="h-56 w-full">
      {comoBarras ? <Bar data={datos} options={opciones} /> : <Line data={datos} options={opciones} />}
    </div>
  );
}

/** Reparto: barras horizontales ordenadas de mayor a menor. */
export function GraficoReparto({ items }: { items: Reparto[] }) {
  const colores = useColoresDelTema();

  const datos = {
    labels: items.map((item) => item.etiqueta),
    datasets: [
      {
        data: items.map((item) => item.valor),
        backgroundColor: colores.serie,
        borderRadius: 4,
        // Barras finas: la tinta no aporta nada por encima de cierto grosor.
        barThickness: 14,
      },
    ],
  };

  const opciones = {
    indexAxis: "y" as const,
    responsive: true,
    maintainAspectRatio: false,
    animation: SIN_ANIMACION_SI_MOLESTA ? (false as const) : undefined,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        beginAtZero: true,
        grid: { color: colores.rejilla },
        ticks: { color: colores.texto, precision: 0 },
        border: { display: false },
      },
      y: {
        grid: { display: false },
        ticks: { color: colores.texto },
        border: { color: colores.rejilla },
      },
    },
  };

  return (
    <div style={{ height: `${Math.max(140, items.length * 34)}px` }} className="w-full">
      <Bar data={datos} options={opciones} />
    </div>
  );
}
