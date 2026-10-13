import { TvMetaRow } from "@filmax/tv-ui";

/** Мета hero Главной: оценка Кинопоиска ярче, затем жанры, год и длительность. */
export const HomeHero = () => (
  <TvMetaRow lead="7.9 КП" parts={["Фэнтези · Приключения · Семейный", "2022", "2 ч 11 мин"]} />
);

/** Мета деталей сериала. */
export const Series = () => <TvMetaRow parts={["2019–2024", "7 сезонов", "Россия", "Драма, Приключения"]} />;
