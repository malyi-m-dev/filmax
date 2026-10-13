import * as React from "react";
import { TvHero } from "@filmax/tv-ui";

/** Холст 960px уменьшен до ширины карточки витрины — в дизайне компонент в натуральную величину. */
const Fit = ({ children }: { children: React.ReactNode }) => <div style={{ zoom: 0.68 }}>{children}</div>;

/** Hero Главной во всю ширину 960, «Смотреть» в фокусе. */
export const EditorsChoice = () => (
  <Fit>
  <div style={{ width: 960 }}>
    <TvHero
      title="Дом на краю карты"
      rating="7.9"
      parts={["Фэнтези · Приключения · Семейный", "2022", "2 ч 11 мин"]}
      backdropUrl="https://picsum.photos/seed/filmax-1/1280/720"
      focused="play"
    />
  </div>
  </Fit>
);

/** Длинное название в две строки, фокус на «Подробнее». */
export const LongTitle = () => (
  <Fit>
  <div style={{ width: 960 }}>
    <TvHero
      title="Хроники Пограничья: Последняя застава"
      rating="8.4"
      parts={["Драма · Приключения", "2024", "7 сезонов"]}
      backdropUrl="https://picsum.photos/seed/filmax-2003/1280/720"
      focused="details"
    />
  </div>
  </Fit>
);
