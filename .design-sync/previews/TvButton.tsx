import { TvButton } from "@filmax/tv-ui";

/** Пара hero Главной: главное действие белое, второе — тёмное. */
export const HeroPair = () => (
  <div style={{ display: "flex", gap: 12 }}>
    <TvButton label="Смотреть" />
    <TvButton label="Подробнее" primary={false} />
  </div>
);

/** Фокус пульта: увеличение 1.08 и белая рамка — на главной и на вторичной. */
export const Focused = () => (
  <div style={{ display: "flex", gap: 20, padding: 6 }}>
    <TvButton label="Смотреть" focused />
    <TvButton label="Подробнее" primary={false} focused />
  </div>
);

/** С иконками — кнопки hero деталей. */
export const WithIcons = () => (
  <div style={{ display: "flex", gap: 12 }}>
    <TvButton label="Продолжить · S4E7" icon="play" />
    <TvButton label="Буду смотреть" icon="bookmarkBorder" primary={false} />
    <TvButton label="Трейлер" icon="movie" primary={false} />
  </div>
);

/** Баннер «нет сети» на Главной — длинная вторичная кнопка с иконкой. */
export const OfflineBanner = () => (
  <TvButton label="Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить" icon="cloudOff" primary={false} />
);
