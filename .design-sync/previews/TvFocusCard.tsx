import { TvFocusCard } from "@filmax/tv-ui";

const tile = (label: string) => (
  <div className="ftv-title-small" style={{ width: 200, height: 112, display: "flex", alignItems: "center", justifyContent: "center", background: "#1f1f1f", borderRadius: "inherit" }}>
    {label}
  </div>
);

/** Фокус на плитке: увеличение 1.08, белая рамка и тёмный ореол. */
export const FocusedVsIdle = () => (
  <div style={{ display: "flex", gap: 24, padding: 12 }}>
    <TvFocusCard shape="card">{tile("Без фокуса")}</TvFocusCard>
    <TvFocusCard shape="card" focused>{tile("В фокусе")}</TvFocusCard>
  </div>
);

/** Рамка держится и на светлом кадре — за счёт ореола. */
export const OnLightImage = () => (
  <div style={{ padding: 12 }}>
    <TvFocusCard shape="poster" focused>
      <div style={{ width: 150, height: 225, borderRadius: "inherit", background: '#eee url("https://picsum.photos/seed/filmax-light/300/450") center/cover' }} />
    </TvFocusCard>
  </div>
);

/** Карточка актёра в деталях: круг 104px с фото, имя под ним; первая в фокусе. */
export const ActorCircle = () => (
  <div style={{ display: "flex", gap: 18, padding: 12 }}>
    {["Клэр Донован", "Томас Рейли"].map((name, index) => (
      <div key={name} style={{ width: 120, display: "flex", flexDirection: "column", alignItems: "center", gap: 10, opacity: index === 0 ? 1 : 0.55 }}>
        <TvFocusCard shape="circle" focused={index === 0}>
          <div style={{ width: 104, height: 104, borderRadius: "50%", background: `#1f1f1f url("https://picsum.photos/seed/filmax-person-${index}/208/208") center/cover` }} />
        </TvFocusCard>
        <span className="ftv-body-medium ftv-color-secondary" style={{ textAlign: "center" }}>{name}</span>
      </div>
    ))}
  </div>
);
