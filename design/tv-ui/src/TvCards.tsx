import * as React from "react";
import { cx, TvProgressBar, TvRatingPill } from "./TvBasics";

/** Фон постера: картинка или ровная подложка, пока её нет. */
function mediaStyle(imageUrl?: string): React.CSSProperties | undefined {
  return imageUrl ? { backgroundImage: `url("${imageUrl}")` } : undefined;
}

export interface TvPosterCardProps {
  /** Название под постером — одна строка с многоточием. */
  title: string;
  /** Мета под названием: «Фильм · 2024». Не задана — строки нет. */
  meta?: string;
  /** URL постера 2:3. */
  posterUrl?: string;
  /** Оценка в пилюле справа сверху, например «8.3». */
  rating?: string;
  /** Фокус пульта: увеличение 1.08, рамка с ореолом. */
  focused?: boolean;
  /**
   * Приглушить карточку до 55% — так в ряду отступают соседи сфокусированной. По умолчанию
   * приглушена любая карточка не в фокусе, как в приложении; false — показать в полную яркость.
   */
  dimmed?: boolean;
  onClick?: () => void;
}

/**
 * Карточка-постер 2:3, 190×285 (TvPosterCard) — ряды Главной и сетки Каталога, один размер везде.
 * Подпись живёт под постером, а не поверх: постер — единственный источник цвета на экране.
 */
export function TvPosterCard({ title, meta, posterUrl, rating, focused, dimmed, onClick }: TvPosterCardProps) {
  const isDimmed = dimmed ?? !focused;
  return (
    <div className={cx("ftv-card ftv-poster-card", isDimmed && "is-dimmed")} onClick={onClick}>
      <div
        role="button"
        tabIndex={0}
        className={cx("ftv-card__media ftv-focusable", focused && "is-focused")}
      >
        <div className="ftv-card__image" style={mediaStyle(posterUrl)} />
        {rating && (
          <div className="ftv-card__rating">
            <TvRatingPill rating={rating} />
          </div>
        )}
      </div>
      <div className="ftv-card__title">{title}</div>
      {meta && <div className="ftv-card__meta">{meta}</div>}
    </div>
  );
}

export interface TvCollectionCardProps {
  /** Название подборки. */
  title: string;
  /** Обложка подборки 2:3. */
  posterUrl?: string;
  focused?: boolean;
  dimmed?: boolean;
  onClick?: () => void;
}

/** Карточка подборки (TvCollectionCard на Главной): тот же постер 2:3, только название, без меты и рейтинга. */
export function TvCollectionCard({ title, posterUrl, focused, dimmed, onClick }: TvCollectionCardProps) {
  return <TvPosterCard title={title} posterUrl={posterUrl} focused={focused} dimmed={dimmed} onClick={onClick} />;
}

export interface TvProgressCardProps {
  /** Название тайтла или серии. */
  title: string;
  /** То, ради чего карточка существует: «S4 · осталось 41 мин». */
  meta?: string;
  /** Кадр 16:9. */
  imageUrl?: string;
  /** Доля просмотренного, 0..1 — белая полоса снизу кадра. */
  progress: number;
  /** continue — 250×141 («Продолжить просмотр»), episode — 236×133 (ряд серий). По умолчанию continue. */
  size?: "continue" | "episode";
  focused?: boolean;
  /** Как у постера: по умолчанию приглушена, если не в фокусе. */
  dimmed?: boolean;
  onClick?: () => void;
}

/**
 * Карточка 16:9 с прогрессом (TvProgressCard) — «Продолжить просмотр», история, серии сезона.
 * Второй и последний тип медиа-карточки в приложении.
 */
export function TvProgressCard({
  title,
  meta,
  imageUrl,
  progress,
  size = "continue",
  focused,
  dimmed,
  onClick,
}: TvProgressCardProps) {
  const isDimmed = dimmed ?? !focused;
  return (
    <div
      className={cx("ftv-card ftv-progress-card", size === "episode" ? "is-episode" : "is-continue", isDimmed && "is-dimmed")}
      onClick={onClick}
    >
      <div role="button" tabIndex={0} className={cx("ftv-card__media ftv-focusable", focused && "is-focused")}>
        <div className="ftv-card__image" style={mediaStyle(imageUrl)}>
          <div className="ftv-card__progress">
            <TvProgressBar progress={progress} />
          </div>
        </div>
      </div>
      <div className="ftv-card__title">{title}</div>
      {meta && <div className="ftv-card__meta">{meta}</div>}
    </div>
  );
}

export interface TvRailProps {
  /** Заголовок ряда: «Продолжить просмотр», «В тренде». */
  title: string;
  /** Карточки ряда — TvPosterCard, TvProgressCard или TvCollectionCard. */
  children: React.ReactNode;
}

/**
 * Горизонтальный ряд карточек с заголовком (TvRail). Поля ряда — безопасные 58px слева и справа и
 * 12px сверху-снизу под увеличение карточки в фокусе; лишнее обрезается краем экрана.
 */
export function TvRail({ title, children }: TvRailProps) {
  return (
    <section className="ftv-rail">
      <h2 className="ftv-rail__title">{title}</h2>
      <div className="ftv-rail__row">{children}</div>
    </section>
  );
}
