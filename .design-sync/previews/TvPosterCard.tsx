import { TvPosterCard } from "@filmax/tv-ui";

const P = "https://picsum.photos/seed";

/** Ряд постеров: средний в фокусе — увеличен, с рамкой; соседи приглушены до 55%. */
export const RowWithFocus = () => (
  <div style={{ display: "flex", gap: 18, padding: 14 }}>
    <TvPosterCard title="Невидимая сторона Луны" meta="Фильм · 2025" posterUrl={`${P}/filmax-12/380/570`} rating="7.2" />
    <TvPosterCard title="Дом на краю карты" meta="Фильм · 2022" posterUrl={`${P}/filmax-1/380/570`} rating="7.9" focused />
    <TvPosterCard title="Кукловод" meta="Сериал · 2024" posterUrl={`${P}/filmax-13/380/570`} rating="7.5" />
  </div>
);

/** В полную яркость, без рейтинга и меты — если их нет у тайтла. */
export const Plain = () => (
  <div style={{ display: "flex", gap: 18 }}>
    <TvPosterCard title="Код доступа: Гелиос" meta="Фильм · 2024" posterUrl={`${P}/filmax-11/380/570`} rating="6.8" dimmed={false} />
    <TvPosterCard title="Последний рейс на Мурманск" posterUrl={`${P}/filmax-14/380/570`} dimmed={false} />
  </div>
);
