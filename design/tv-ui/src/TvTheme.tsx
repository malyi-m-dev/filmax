import * as React from "react";

export interface TvThemeProps {
  /** Залить фон тёмной подложкой #0A0A0A. По умолчанию true. */
  background?: boolean;
  className?: string;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

/**
 * Корень темы Filmax TV (FilmaxTvTheme): шрифт Roboto, основной цвет текста и тёмная подложка.
 * Оборачивает всё, что рисуется не внутри TvScreen, — иначе текст возьмёт шрифт браузера.
 */
export function TvTheme({ background = true, className, style, children }: TvThemeProps) {
  return (
    <div
      className={["ftv-root", className].filter(Boolean).join(" ")}
      style={{ background: background ? "var(--ftv-surface)" : undefined, ...style }}
    >
      {children}
    </div>
  );
}
