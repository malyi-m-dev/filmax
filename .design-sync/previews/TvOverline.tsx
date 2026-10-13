import { TvOverline } from "@filmax/tv-ui";

/** Надзаголовок hero поверх постера и заголовок группы настроек на ровном фоне. */
export const Tones = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
    <TvOverline text="Выбор редакции" />
    <TvOverline text="Аккаунт" tone="dim" />
  </div>
);
