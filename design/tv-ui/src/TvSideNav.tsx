import * as React from "react";
import { cx } from "./TvBasics";
import { TvIcon, type TvIconName } from "./TvIcon";

/** Раздел приложения — четыре на обеих платформах. */
export type TvSection = "home" | "catalog" | "mine" | "profile";

const SECTIONS: Array<{ id: TvSection; label: string; icon: TvIconName }> = [
  { id: "home", label: "Главная", icon: "home" },
  { id: "catalog", label: "Каталог", icon: "catalog" },
  { id: "mine", label: "Моё", icon: "bookmark" },
  { id: "profile", label: "Профиль", icon: "person" },
];

const ITEM_HEIGHT = 44;
const ITEM_GAP = 8;
/** Край капли со стороны движения быстрее хвоста — подсветка вытягивается и подтягивается. */
const LEADING_EDGE_MS = 180;
const TRAILING_EDGE_MS = 420;

export interface TvSideNavProps {
  /** Открытый раздел — в свёрнутом меню у него тихая подложка и белая иконка. */
  active: TvSection;
  /** Меню раскрыто (в нём фокус): подписи, белая подсветка, экран затемнён. */
  expanded?: boolean;
  /** Пункт под фокусом в раскрытом меню. По умолчанию — открытый раздел. */
  focusedSection?: TvSection;
  /** Инициалы для пункта «Профиль» вместо иконки. */
  initials?: string;
  onSelect?: (section: TvSection) => void;
}

/**
 * Боковое меню разделов (TvSideNav): свёрнутое — колонка иконок 56px в левом поле поверх
 * контента, раскрытое — 240px с подписями и затемнением экрана. Подсветка одна на всё меню и
 * перетекает между пунктами каплей. Ставится внутрь TvScreen, который сам его рисует.
 */
export function TvSideNav({ active, expanded, focusedSection, initials, onSelect }: TvSideNavProps) {
  const target = expanded ? (focusedSection ?? active) : active;
  const index = Math.max(0, SECTIONS.findIndex((section) => section.id === target));
  const previous = React.useRef(index);
  const movingDown = index >= previous.current;
  React.useEffect(() => {
    previous.current = index;
  }, [index]);

  const top = index * (ITEM_HEIGHT + ITEM_GAP);
  const listHeight = SECTIONS.length * ITEM_HEIGHT + (SECTIONS.length - 1) * ITEM_GAP;
  const highlightStyle: React.CSSProperties = {
    top,
    bottom: listHeight - top - ITEM_HEIGHT,
    transitionDuration: movingDown
      ? `${TRAILING_EDGE_MS}ms, ${LEADING_EDGE_MS}ms, 180ms, 200ms, 200ms`
      : `${LEADING_EDGE_MS}ms, ${TRAILING_EDGE_MS}ms, 180ms, 200ms, 200ms`,
  };

  return (
    <nav className={cx("ftv-side-nav", expanded && "is-expanded")}>
      <div className="ftv-side-nav__scrim is-collapsed" />
      <div className="ftv-side-nav__scrim is-expanded" />
      <div className="ftv-side-nav__panel">
        <div className="ftv-side-nav__list">
          <div className="ftv-side-nav__highlight" style={highlightStyle} />
          {SECTIONS.map((section) => (
            <div
              key={section.id}
              role="button"
              tabIndex={0}
              onClick={() => onSelect?.(section.id)}
              className={cx(
                "ftv-side-nav__item",
                section.id === active && "is-active",
                expanded && section.id === target && "is-focused",
              )}
            >
              {section.id === "profile" && initials ? (
                <span className="ftv-side-nav__initials">{initials}</span>
              ) : (
                <TvIcon name={section.icon} size={22} />
              )}
              <span className="ftv-side-nav__label">{section.label}</span>
            </div>
          ))}
        </div>
      </div>
    </nav>
  );
}

export interface TvScreenProps {
  /** Открытый раздел для бокового меню. */
  section?: TvSection;
  /** Показывать боковое меню — только на экранах разделов; детали и плеер без него. По умолчанию true. */
  showNav?: boolean;
  /** Меню раскрыто (фокус в нём). */
  navExpanded?: boolean;
  /** Пункт меню под фокусом. */
  navFocused?: TvSection;
  /** Инициалы профиля в меню. По умолчанию «ДЗ». */
  initials?: string;
  /** Контент экрана. Отступы раздела: слева 58px, сверху 40px — их задаёт сам контент. */
  children?: React.ReactNode;
}

/**
 * Кадр ТВ-экрана 960×540 (1080p при density 2): фон #0A0A0A, контент и боковое меню поверх него.
 * Любой ТВ-экран собирается внутри TvScreen.
 */
export function TvScreen({
  section = "home",
  showNav = true,
  navExpanded,
  navFocused,
  initials = "ДЗ",
  children,
}: TvScreenProps) {
  return (
    <div className="ftv-root ftv-screen">
      <div className="ftv-screen__content">{children}</div>
      {showNav && (
        <TvSideNav active={section} expanded={navExpanded} focusedSection={navFocused} initials={initials} />
      )}
    </div>
  );
}
