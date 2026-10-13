import { TvChip } from "@filmax/tv-ui";

/** Ряд типа в Каталоге: «Все» выбран. */
export const TypeRow = () => (
  <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
    <TvChip label="Все" selected />
    <TvChip label="Фильмы" />
    <TvChip label="Сериалы" />
    <TvChip label="Аниме" />
    <TvChip label="Документальные" />
  </div>
);

/** Фокус — рамка поверх: на невыбранном и на выбранном чипе. */
export const Focused = () => (
  <div style={{ display: "flex", gap: 16, padding: 6 }}>
    <TvChip label="Комедия" focused />
    <TvChip label="Сезон 4" selected focused />
  </div>
);

/** Сезоны сериала в деталях. */
export const Seasons = () => (
  <div style={{ display: "flex", gap: 10 }}>
    {[1, 2, 3, 4, 5, 6, 7].map((season) => (
      <TvChip key={season} label={`Сезон ${season}`} selected={season === 4} />
    ))}
  </div>
);
