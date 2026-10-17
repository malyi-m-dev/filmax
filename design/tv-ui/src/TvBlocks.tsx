import * as React from "react";
import { cx, TvButton, TvMetaRow, TvOverline } from "./TvBasics";
import { TvIcon } from "./TvIcon";

export interface TvErrorStateProps {
  /** Заголовок: «Не удалось загрузить». */
  title: string;
  /** Пояснение — держится в колонке до 620px. */
  message: string;
  /** Подпись кнопки повтора. Не задана — кнопки нет (ошибка, из которой повтор не выводит). */
  retryLabel?: string;
  /** Кнопка повтора в фокусе — на ТВ она забирает фокус сама. По умолчанию true. */
  retryFocused?: boolean;
  onRetry?: () => void;
}

/** Экран целиком не загрузился (TvErrorState): заголовок, пояснение и «Повторить» по центру. */
export function TvErrorState({ title, message, retryLabel, retryFocused = true, onRetry }: TvErrorStateProps) {
  return (
    <div className="ftv-error-state">
      <div className="ftv-headline-small ftv-color-primary">{title}</div>
      <div className="ftv-error-state__message ftv-body-large ftv-color-secondary">{message}</div>
      {retryLabel && (
        <div className="ftv-error-state__action">
          <TvButton label={retryLabel} focused={retryFocused} onClick={onRetry} />
        </div>
      )}
    </div>
  );
}

export interface TvExpandableTextProps {
  /** Длинный текст — описание тайтла. */
  text: string;
  /** Сколько строк видно в свёрнутом виде. По умолчанию 4. */
  collapsedLines?: number;
  /** Развёрнут целиком. */
  expanded?: boolean;
  /** Фокус пульта: подложка #141414 и рамка, без увеличения — абзац не раздувается. */
  focused?: boolean;
  /** Показывать «Подробнее/Свернуть» — только если текст правда не влез. По умолчанию true. */
  toggleable?: boolean;
  onToggle?: () => void;
}

/**
 * Длинный текст под пульт (TvExpandableText): свёрнут до 4 строк, «ОК» разворачивает. Внутри поле
 * 12px под рамку фокуса — снаружи его вычитают, чтобы текст стоял в линию с соседними секциями.
 */
export function TvExpandableText({
  text,
  collapsedLines = 4,
  expanded,
  focused,
  toggleable = true,
  onToggle,
}: TvExpandableTextProps) {
  return (
    <div
      role={toggleable ? "button" : undefined}
      tabIndex={toggleable ? 0 : undefined}
      onClick={toggleable ? onToggle : undefined}
      className={cx("ftv-expandable", toggleable && "ftv-focusable is-flat", focused && "is-focused")}
      style={{ borderRadius: "var(--ftv-radius-card)" }}
    >
      <div className="ftv-expandable__text" style={expanded ? undefined : { WebkitLineClamp: collapsedLines }}>
        {text}
      </div>
      {toggleable && <div className="ftv-expandable__toggle">{expanded ? "Свернуть" : "Подробнее"}</div>}
    </div>
  );
}

export interface TvSearchBarProps {
  /** Введённый запрос. Пусто — плейсхолдер «Название фильма или сериала». */
  query?: string;
  /** Что в фокусе: field — строка, voice — кнопка голоса, null — ничего. */
  focused?: "field" | "voice" | null;
  /** Режим ввода: строка стала полем, открыта системная клавиатура, мигает курсор. */
  editing?: boolean;
  onClick?: () => void;
  onVoice?: () => void;
}

/**
 * Строка поиска Каталога (CatalogSearchBar): поле 56px и кнопка голосового поиска рядом. В фокусе
 * строка лишь слегка увеличивается (1.02) — фокус держит белая рамка.
 */
export function TvSearchBar({ query = "", focused = null, editing, onClick, onVoice }: TvSearchBarProps) {
  const empty = query.length === 0;
  return (
    <div className="ftv-search">
      <div
        role="button"
        tabIndex={0}
        onClick={onClick}
        className={cx(
          "ftv-search__bar",
          editing ? "is-editing" : "ftv-focusable is-wide",
          !editing && focused === "field" && "is-focused",
        )}
      >
        <TvIcon name="search" size={20} className="ftv-color-dim" />
        <span className={cx("ftv-search__text", empty && !editing ? "ftv-color-dim" : "ftv-color-primary")}>
          {empty && !editing ? "Название фильма или сериала" : query}
          {editing && <span className="ftv-search__caret" />}
        </span>
      </div>
      <div
        role="button"
        tabIndex={0}
        aria-label="Голосовой поиск"
        onClick={onVoice}
        className={cx("ftv-search__voice ftv-focusable", focused === "voice" && "is-focused")}
      >
        <TvIcon name="mic" size={24} />
      </div>
    </div>
  );
}

export interface TvSettingRowProps {
  /** Название настройки слева. */
  label: string;
  /** Текущее значение справа, вторичным цветом. */
  value?: string;
  /** Фокус: подложка светлее и белая рамка, без увеличения — строка широкая. */
  focused?: boolean;
  /** Деструктивное действие («Выйти»): подпись приглушённым красным. */
  destructive?: boolean;
  onClick?: () => void;
}

/** Строка настройки Профиля (SettingRow): 60px, панель #141414, подпись слева и значение справа. */
export function TvSettingRow({ label, value, focused, destructive, onClick }: TvSettingRowProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      className={cx("ftv-setting-row", focused && "is-focused", destructive && "is-destructive")}
    >
      <span className="ftv-setting-row__label">{label}</span>
      {value && <span className="ftv-setting-row__value">{value}</span>}
    </div>
  );
}

export interface TvDialogProps {
  /** Заголовок вопроса: «Удалить папку?». */
  title: string;
  /** Пояснение последствий. */
  message: string;
  /** Подпись подтверждения — главная белая кнопка. */
  confirmLabel: string;
  /** Подпись отмены. По умолчанию «Отмена». */
  cancelLabel?: string;
  /** Какая кнопка в фокусе. По умолчанию confirm — диалог открывается с фокусом на ней. */
  focused?: "confirm" | "cancel";
  /** Затемнить экран под диалогом и поставить его по центру родителя (position: relative). */
  overlay?: boolean;
  onConfirm?: () => void;
  onCancel?: () => void;
}

/** Диалог подтверждения (TvConfirmDialog): панель 420px, заголовок, пояснение и две кнопки. */
export function TvDialog({
  title,
  message,
  confirmLabel,
  cancelLabel = "Отмена",
  focused = "confirm",
  overlay,
  onConfirm,
  onCancel,
}: TvDialogProps) {
  const panel = (
    <div className="ftv-dialog" role="dialog">
      <div className="ftv-title-large ftv-color-primary">{title}</div>
      <div className="ftv-dialog__message ftv-body-large ftv-color-secondary">{message}</div>
      <div className="ftv-dialog__actions">
        <TvButton label={confirmLabel} focused={focused === "confirm"} onClick={onConfirm} />
        <TvButton label={cancelLabel} primary={false} focused={focused === "cancel"} onClick={onCancel} />
      </div>
    </div>
  );
  return overlay ? <div className="ftv-dialog-overlay">{panel}</div> : panel;
}

export interface TvHeroProps {
  /** Название тайтла — до двух строк. */
  title: string;
  /** Надзаголовок. По умолчанию «Выбор редакции». */
  overline?: string;
  /** Оценка Кинопоиска, выводится первой и ярче: «7.9 КП». */
  rating?: string;
  /** Мета: жанры через « · », год, длительность. */
  parts: string[];
  /** Широкий кадр-бэкдроп. */
  backdropUrl?: string;
  /** Какая кнопка в фокусе: play («Смотреть»), details («Подробнее») или ничего. */
  focused?: "play" | "details" | null;
  onPlay?: () => void;
  onDetails?: () => void;
}

/**
 * Hero Главной (TvHero): кадр 326px во всю ширину, затемнённый слева и снизу, и блок 520px
 * у левого поля — надзаголовок, название 44px, мета и кнопки «Смотреть» / «Подробнее».
 */
export function TvHero({
  title,
  overline = "Выбор редакции",
  rating,
  parts,
  backdropUrl,
  focused = null,
  onPlay,
  onDetails,
}: TvHeroProps) {
  return (
    <div className="ftv-hero is-home">
      <div className="ftv-hero__backdrop" style={backdropUrl ? { backgroundImage: `url("${backdropUrl}")` } : undefined} />
      <div className="ftv-hero__scrim is-horizontal" />
      <div className="ftv-hero__scrim is-vertical" />
      <div className="ftv-hero__content">
        <TvOverline text={overline} />
        <div className="ftv-hero__title ftv-display-medium">{title}</div>
        <div className="ftv-hero__meta">
          <TvMetaRow lead={rating ? `${rating} КП` : undefined} parts={parts} />
        </div>
        <div className="ftv-hero__actions">
          <TvButton label="Смотреть" focused={focused === "play"} onClick={onPlay} />
          <TvButton label="Подробнее" primary={false} focused={focused === "details"} onClick={onDetails} />
        </div>
      </div>
    </div>
  );
}

/** Оценка из источника: значение и название («7.9 Кинопоиск»). */
export interface TvRatingSource {
  value: string;
  source: string;
}

export interface TvDetailsHeroProps {
  /** Название тайтла — 40px, до двух строк. */
  title: string;
  /** Мета: год, длительность или число сезонов, страна, жанры. */
  parts: string[];
  /** Оценки через тонкий разделитель: Кинопоиск, IMDb. */
  ratings?: TvRatingSource[];
  /** Широкий кадр-бэкдроп. */
  backdropUrl?: string;
  /** Подпись главной кнопки: «Смотреть» или «Продолжить · S4E7». */
  playLabel?: string;
  /** Тайтл уже в «Буду смотреть»: кнопка «В списке» с заполненной закладкой. */
  inList?: boolean;
  /** Есть трейлер — третья кнопка «Трейлер». */
  hasTrailer?: boolean;
  /** Какая кнопка в фокусе. */
  focused?: "play" | "list" | "trailer" | null;
}

/**
 * Hero деталей (DetailsHero): кадр 346px, блок 600px у левого поля — название, мета, оценки и
 * кнопки «Смотреть» / «Буду смотреть» / «Трейлер».
 */
export function TvDetailsHero({
  title,
  parts,
  ratings = [],
  backdropUrl,
  playLabel = "Смотреть",
  inList,
  hasTrailer,
  focused = null,
}: TvDetailsHeroProps) {
  return (
    <div className="ftv-hero is-details">
      <div className="ftv-hero__backdrop" style={backdropUrl ? { backgroundImage: `url("${backdropUrl}")` } : undefined} />
      <div className="ftv-hero__scrim is-horizontal" />
      <div className="ftv-hero__scrim is-vertical" />
      <div className="ftv-hero__content">
        <div className="ftv-hero__title ftv-display-small">{title}</div>
        <div className="ftv-hero__meta">
          <TvMetaRow parts={parts} />
        </div>
        {ratings.length > 0 && (
          <div className="ftv-hero__ratings ftv-ratings">
            {ratings.map((rating, index) => (
              <React.Fragment key={rating.source}>
                {index > 0 && <span className="ftv-ratings__divider" />}
                <span className="ftv-ratings__item">
                  <span className="ftv-ratings__value">{rating.value}</span>
                  <span className="ftv-ratings__source">{rating.source}</span>
                </span>
              </React.Fragment>
            ))}
          </div>
        )}
        <div className="ftv-hero__actions">
          <TvButton label={playLabel} icon="play" focused={focused === "play"} />
          <TvButton
            label={inList ? "В списке" : "Буду смотреть"}
            icon={inList ? "bookmark" : "bookmarkBorder"}
            primary={false}
            focused={focused === "list"}
          />
          {hasTrailer && <TvButton label="Трейлер" icon="movie" primary={false} focused={focused === "trailer"} />}
        </div>
      </div>
    </div>
  );
}
