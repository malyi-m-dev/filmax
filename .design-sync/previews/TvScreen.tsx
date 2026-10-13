import * as React from "react";
import { TvScreen, TvHero, TvRail, TvProgressCard, TvPosterCard, TvSearchBar, TvChip } from "@filmax/tv-ui";

const P = "https://picsum.photos/seed";

/** Холст 960px уменьшен до ширины карточки витрины — в дизайне компонент в натуральную величину. */
const Fit = ({ children }: { children: React.ReactNode }) => <div style={{ zoom: 0.68 }}>{children}</div>;

/** Главная: hero «Выбор редакции» и ряд «Продолжить просмотр», меню свёрнуто. */
export const Home = () => (
  <Fit>
  <TvScreen section="home" initials="ДЗ">
    <div style={{ paddingTop: 40 }}>
      <TvHero
        title="Дом на краю карты"
        rating="7.9"
        parts={["Фэнтези · Приключения · Семейный", "2022", "2 ч 11 мин"]}
        backdropUrl={`${P}/filmax-1/1280/720`}
        focused="play"
      />
      <div style={{ marginTop: 24 }}>
        <TvRail title="Продолжить просмотр">
          <TvProgressCard title="Хроники Пограничья" meta="S4 · осталось 41 мин" imageUrl={`${P}/filmax-2003/500/282`} progress={0.62} />
          <TvProgressCard title="Код доступа: Гелиос" meta="осталось 1 ч 56 мин" imageUrl={`${P}/filmax-2004/500/282`} progress={0.2} />
          <TvProgressCard title="Доставка до двери" meta="S1 · осталось 7 мин" imageUrl={`${P}/filmax-2005/500/282`} progress={0.9} />
        </TvRail>
      </div>
    </div>
  </TvScreen>
  </Fit>
);

/** Каталог с раскрытым меню: фокус на «Каталог», экран затемнён. */
export const CatalogNavOpen = () => (
  <Fit>
  <TvScreen section="catalog" navExpanded navFocused="catalog" initials="ДЗ">
    <div style={{ padding: "40px 58px 0" }}>
      <TvSearchBar />
      <div style={{ display: "flex", gap: 10, marginTop: 18 }}>
        <TvChip label="Все" selected />
        <TvChip label="Фильмы" />
        <TvChip label="Сериалы" />
        <TvChip label="Аниме" />
      </div>
      <div style={{ display: "flex", gap: 18, marginTop: 30 }}>
        {[10, 11, 12, 13].map((id) => (
          <TvPosterCard key={id} title={`Тайтл ${id}`} meta="Фильм · 2024" posterUrl={`${P}/filmax-${id}/380/570`} rating="7.5" dimmed={false} />
        ))}
      </div>
    </div>
  </TvScreen>
  </Fit>
);
