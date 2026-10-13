import * as React from "react";
import { TvRail, TvPosterCard } from "@filmax/tv-ui";

const P = "https://picsum.photos/seed";

/** Холст 960px уменьшен до ширины карточки витрины — в дизайне компонент в натуральную величину. */
const Fit = ({ children }: { children: React.ReactNode }) => <div style={{ zoom: 0.68 }}>{children}</div>;

/** Ряд «В тренде» во всю ширину экрана 960: поля 58px, вторая карточка в фокусе. */
export const Trending = () => (
  <Fit>
  <div style={{ width: 960, background: "#0a0a0a", paddingTop: 12 }}>
    <TvRail title="В тренде">
      {[10, 11, 12, 13, 14].map((id) => (
        <TvPosterCard key={id} title={["Дом на краю карты", "Код доступа: Гелиос", "Невидимая сторона Луны", "Кукловод", "Последний рейс"][id - 10]} meta="Фильм · 2024" posterUrl={`${P}/filmax-${id}/380/570`} rating="7.4" focused={id === 11} />
      ))}
    </TvRail>
  </div>
  </Fit>
);
