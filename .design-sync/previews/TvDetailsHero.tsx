import * as React from "react";
import { TvDetailsHero } from "@filmax/tv-ui";

/** Холст 960px уменьшен до ширины карточки витрины — в дизайне компонент в натуральную величину. */
const Fit = ({ children }: { children: React.ReactNode }) => <div style={{ zoom: 0.68 }}>{children}</div>;

/** Сериал с прогрессом: «Продолжить · S4E7» в фокусе, есть трейлер. */
export const SeriesResume = () => (
  <Fit>
  <div style={{ width: 960 }}>
    <TvDetailsHero
      title="Хроники Пограничья"
      parts={["2019–2024", "7 сезонов", "Россия", "Драма, Приключения"]}
      ratings={[{ value: "8.4", source: "Кинопоиск" }, { value: "8.1", source: "IMDb" }]}
      backdropUrl="https://picsum.photos/seed/filmax-2003/1280/720"
      playLabel="Продолжить · S4E7"
      hasTrailer
      focused="play"
    />
  </div>
  </Fit>
);

/** Фильм в «Буду смотреть», без трейлера; фокус на закладке. */
export const MovieInList = () => (
  <Fit>
  <div style={{ width: 960 }}>
    <TvDetailsHero
      title="Дом на краю карты"
      parts={["2022", "2 ч 11 мин", "Великобритания", "Фэнтези, Приключения"]}
      ratings={[{ value: "7.9", source: "Кинопоиск" }, { value: "7.6", source: "IMDb" }]}
      backdropUrl="https://picsum.photos/seed/filmax-1/1280/720"
      inList
      focused="list"
    />
  </div>
  </Fit>
);
