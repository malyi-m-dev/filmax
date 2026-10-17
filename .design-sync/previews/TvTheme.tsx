import { TvTheme } from "@filmax/tv-ui";

/** Палитра монохрома: поверхности и уровни текста на подложке темы. */
export const Palette = () => (
  <TvTheme style={{ padding: 20, display: "flex", flexDirection: "column", gap: 14 }}>
    <div style={{ display: "flex", gap: 10 }}>
      {["#0a0a0a", "#141414", "#1f1f1f", "#2e2e2e", "#ffffff"].map((color) => (
        <div key={color} style={{ width: 92, height: 56, borderRadius: 10, background: color, border: "1px solid #2e2e2e" }} />
      ))}
    </div>
    <div className="ftv-title-medium ftv-color-primary">Основной текст #E8E8E8</div>
    <div className="ftv-body-large ftv-color-secondary">Вторичный текст и мета #A0A0A0</div>
    <div className="ftv-body-small ftv-color-dim">Третичный текст, плейсхолдеры #8A8A8A</div>
    <div className="ftv-body-small ftv-color-error">Ошибки и деструктив #E0736B</div>
  </TvTheme>
);

/** Шкала типографики TV: от названия hero до надзаголовка. */
export const Typography = () => (
  <TvTheme style={{ padding: 20, display: "flex", flexDirection: "column", gap: 8 }}>
    <div className="ftv-display-medium">Дом на краю карты</div>
    <div className="ftv-headline-medium">Каталог</div>
    <div className="ftv-title-large">Продолжить просмотр</div>
    <div className="ftv-title-medium">Качество видео</div>
    <div className="ftv-body-large ftv-color-secondary">2022 · 2 ч 11 мин · Фэнтези</div>
    <div className="ftv-label-medium ftv-color-secondary">Выбор редакции</div>
  </TvTheme>
);
