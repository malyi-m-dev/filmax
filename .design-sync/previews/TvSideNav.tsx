import { TvSideNav } from "@filmax/tv-ui";

const frame = { position: "relative" as const, width: 300, height: 300, background: "#1f1f1f" };

/** Свёрнутое: колонка иконок, у открытого раздела тихая подложка. */
export const Collapsed = () => (
  <div style={frame}>
    <TvSideNav active="home" initials="ДЗ" />
  </div>
);

/** Раскрытое: фокус в меню на открытом разделе — белая подсветка, подписи, затемнение. */
export const Expanded = () => (
  <div style={frame}>
    <TvSideNav active="home" expanded focusedSection="home" initials="ДЗ" />
  </div>
);

/** Пульт ушёл на другой пункт: подсветка на «Моё», открытый раздел «Главная» остаётся белым. */
export const ExpandedOtherFocused = () => (
  <div style={frame}>
    <TvSideNav active="home" expanded focusedSection="mine" initials="ДЗ" />
  </div>
);

/** Открыт Каталог, меню свёрнуто. */
export const CatalogActive = () => (
  <div style={frame}>
    <TvSideNav active="catalog" initials="ДЗ" />
  </div>
);
