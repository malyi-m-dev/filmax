import { TvIcon } from "@filmax/tv-ui";

const names = ["home", "catalog", "bookmark", "bookmarkBorder", "person", "play", "movie", "search", "mic", "check", "cloudOff"] as const;

/** Весь набор иконок ТВ-приложения, 22px. */
export const AllIcons = () => (
  <div style={{ display: "grid", gridTemplateColumns: "repeat(6, 80px)", gap: 16, color: "#e8e8e8" }}>
    {names.map((name) => (
      <div key={name} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
        <TvIcon name={name} size={28} />
        <span className="ftv-body-small ftv-color-secondary">{name}</span>
      </div>
    ))}
  </div>
);

/** Иконки берут цвет текста: вторичный, основной и на белой заливке. */
export const Tones = () => (
  <div style={{ display: "flex", gap: 20, alignItems: "center" }}>
    <span style={{ color: "#8a8a8a" }}><TvIcon name="search" size={20} /></span>
    <span style={{ color: "#e8e8e8" }}><TvIcon name="mic" size={24} /></span>
    <span style={{ color: "#0a0a0a", background: "#fff", borderRadius: 22, padding: "11px 9px" }}><TvIcon name="home" /></span>
  </div>
);
