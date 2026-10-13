import { TvSearchBar } from "@filmax/tv-ui";

const wide = { width: 600 };

/** Пустая строка в фокусе: рамка, едва заметное увеличение. */
export const FieldFocused = () => (
  <div style={{ ...wide, padding: 8 }}>
    <TvSearchBar focused="field" />
  </div>
);

/** Режим ввода: открыта клавиатура, мигает курсор. */
export const Editing = () => (
  <div style={wide}>
    <TvSearchBar query="хроники" editing />
  </div>
);

/** Фокус на голосовом поиске, в строке прежний запрос. */
export const VoiceFocused = () => (
  <div style={{ ...wide, padding: 8 }}>
    <TvSearchBar query="Хроники Пограничья" focused="voice" />
  </div>
);
