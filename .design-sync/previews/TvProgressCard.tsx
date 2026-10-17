import { TvProgressCard } from "@filmax/tv-ui";

const P = "https://picsum.photos/seed";

/** «Продолжить просмотр»: 250×141, первая в фокусе. */
export const ContinueRow = () => (
  <div style={{ display: "flex", gap: 18, padding: 14 }}>
    <TvProgressCard title="Хроники Пограничья" meta="S4 · осталось 41 мин" imageUrl={`${P}/filmax-2003/500/282`} progress={0.62} focused />
    <TvProgressCard title="Код доступа: Гелиос" meta="осталось 1 ч 56 мин" imageUrl={`${P}/filmax-2004/500/282`} progress={0.15} />
  </div>
);

/** Серии сезона: 236×133 — начатая и две следующие. */
export const Episodes = () => (
  <div style={{ display: "flex", gap: 18 }}>
    <TvProgressCard size="episode" title="Корона без короля" meta="Серия 7 · осталось 41 мин" imageUrl={`${P}/filmax-ep-2003-4-7/472/266`} progress={0.4} dimmed={false} />
    <TvProgressCard size="episode" title="Всё, что мы не успели сказать" meta="Серия 8 · 54 мин" imageUrl={`${P}/filmax-ep-2003-4-8/472/266`} progress={0} dimmed={false} />
    <TvProgressCard size="episode" title="Долгая зима" meta="Серия 9 · 46 мин" imageUrl={`${P}/filmax-ep-2003-4-9/472/266`} progress={0} dimmed={false} />
  </div>
);
