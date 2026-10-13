import { TvSettingRow, TvOverline } from "@filmax/tv-ui";

/** Группа настроек Профиля: вторая строка в фокусе, «Выйти» — деструктив. */
export const ProfileGroup = () => (
  <div style={{ width: 640, display: "flex", flexDirection: "column", gap: 10 }}>
    <TvOverline text="Просмотр" tone="dim" />
    <TvSettingRow label="Качество видео" value="Авто" />
    <TvSettingRow label="Озвучка по умолчанию" value="Дубляж" focused />
    <TvSettingRow label="Проверить обновления" value="1.10.0" />
    <TvSettingRow label="Выйти" destructive />
  </div>
);
