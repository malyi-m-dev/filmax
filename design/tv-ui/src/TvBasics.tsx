import * as React from "react";
import { TvIcon, type TvIconName } from "./TvIcon";

/** Склеивает классы, пропуская пустые. */
export function cx(...names: Array<string | false | null | undefined>): string {
  return names.filter(Boolean).join(" ");
}

export interface TvButtonProps {
  /** Подпись кнопки — одна строка, без переноса. */
  label: string;
  /** Главное действие: белая заливка и тёмный текст. false — тёмная вторичная кнопка. По умолчанию true. */
  primary?: boolean;
  /** Иконка слева от подписи (play, bookmark, bookmarkBorder, movie, cloudOff…). */
  icon?: TvIconName;
  /** Состояние фокуса пульта: увеличение 1.08 и белая рамка. */
  focused?: boolean;
  onClick?: () => void;
  className?: string;
}

/**
 * Кнопка для пульта (TvButton). Primary — главное действие экрана («Смотреть»), белая заливка;
 * вторичная — тёмная поверхность #1F1F1F. Цветных кнопок в Filmax нет: акцент — белый.
 */
export function TvButton({ label, primary = true, icon, focused, onClick, className }: TvButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cx(
        "ftv-button ftv-focusable",
        primary ? "is-primary" : "is-secondary",
        icon && "has-icon",
        focused && "is-focused",
        className,
      )}
    >
      {icon && <TvIcon name={icon} size={20} />}
      <span>{label}</span>
    </button>
  );
}

/** Форма фокусируемой поверхности — токены радиусов TvMetrics. */
export type TvShape = "poster" | "card" | "panel" | "button" | "chip" | "circle";

const RADIUS: Record<TvShape, string> = {
  poster: "var(--ftv-radius-poster)",
  card: "var(--ftv-radius-card)",
  panel: "var(--ftv-radius-panel)",
  button: "var(--ftv-radius-button)",
  chip: "var(--ftv-radius-chip)",
  circle: "50%",
};

export interface TvFocusCardProps {
  /** Состояние фокуса: увеличение, белая рамка 3px и тёмный ореол снаружи неё. */
  focused?: boolean;
  /** Скругление по токенам: poster 8, card 10, panel 12, button 9, chip 20; circle — фото актёра. По умолчанию card. */
  shape?: TvShape;
  /** Широкий элемент (строка поиска, панель во всю ширину): в фокусе 1.02 вместо 1.08. */
  wide?: boolean;
  onClick?: () => void;
  className?: string;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}

/**
 * Фокусируемый контейнер (TvFocusCard) — основа всех карточек и плиток. Фокус на ТВ — это три
 * сигнала сразу: масштаб, белая рамка и тёмный ореол, который держит рамку видимой на светлом постере.
 */
export function TvFocusCard({ focused, shape = "card", wide, onClick, className, style, children }: TvFocusCardProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      className={cx("ftv-focus-card ftv-focusable", wide && "is-wide", focused && "is-focused", className)}
      style={{ borderRadius: RADIUS[shape], ...style }}
    >
      {children}
    </div>
  );
}

export interface TvChipProps {
  /** Подпись: тип, жанр, сортировка, сезон, сегмент «Моё». */
  label: string;
  /** Выбран: белая заливка с тёмным текстом. Выбор и фокус — разные сигналы. */
  selected?: boolean;
  /** Фокус пульта: рамка с ореолом и увеличение. */
  focused?: boolean;
  onClick?: () => void;
}

/**
 * Чип-фильтр (TvChip). Выбранный — белый, невыбранный — #1F1F1F с серым текстом. Фокус рисуется
 * рамкой поверх, поэтому выбранный чип в фокусе отличим от просто выбранного.
 */
export function TvChip({ label, selected, focused, onClick }: TvChipProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      className={cx("ftv-chip ftv-focusable", selected && "is-selected", focused && "is-focused")}
    >
      {label}
    </div>
  );
}

export interface TvRatingPillProps {
  /** Готовая строка оценки, например «8.3». */
  rating: string;
}

/** Пилюля рейтинга поверх постера (TvRatingPill): тёмная полупрозрачная подложка, оценка цветом не кодируется. */
export function TvRatingPill({ rating }: TvRatingPillProps) {
  return <span className="ftv-rating-pill">{rating}</span>;
}

export interface TvProgressBarProps {
  /** Доля просмотренного, 0..1. */
  progress: number;
}

/** Полоса прогресса на карточке (TvProgressBar): белая заливка по прозрачно-белому треку, 3px. */
export function TvProgressBar({ progress }: TvProgressBarProps) {
  const percent = Math.min(1, Math.max(0, progress)) * 100;
  return (
    <div className="ftv-progress-bar">
      <div className="ftv-progress-bar__fill" style={{ width: `${percent}%` }} />
    </div>
  );
}

export interface TvOverlineProps {
  /** Текст — выводится в верхнем регистре с трекингом 2px. */
  text: string;
  /** secondary (#A0A0A0, по умолчанию — поверх постера) или dim (#8A8A8A — группы на ровном фоне). */
  tone?: "secondary" | "dim";
}

/** Надзаголовок секции (TvOverline): «ВЫБОР РЕДАКЦИИ», «АККАУНТ». */
export function TvOverline({ text, tone = "secondary" }: TvOverlineProps) {
  return <div className={cx("ftv-overline", tone === "dim" ? "ftv-color-dim" : "ftv-color-secondary")}>{text}</div>;
}

export interface TvMetaRowProps {
  /** Части меты: «2024», «Фантастика · Драма», «2 ч 46 мин». Пустые пропускаются. */
  parts: string[];
  /** Выделенная первая часть — оценка «7.9 КП» на hero Главной. */
  lead?: string;
}

/** Строка меты (TvMetaRow): части через отступ 14px, 16px, вторичный цвет. */
export function TvMetaRow({ parts, lead }: TvMetaRowProps) {
  return (
    <div className="ftv-meta-row">
      {lead && <span className="ftv-meta-row__lead">{lead}</span>}
      {parts
        .filter((part) => part.trim().length > 0)
        .map((part) => (
          <span key={part}>{part}</span>
        ))}
    </div>
  );
}
