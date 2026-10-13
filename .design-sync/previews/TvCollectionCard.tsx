import { TvCollectionCard } from "@filmax/tv-ui";

const P = "https://picsum.photos/seed";

/** Ряд «Подборки» на Главной: только название, первая в фокусе. */
export const CollectionsRow = () => (
  <div style={{ display: "flex", gap: 18, padding: 14 }}>
    <TvCollectionCard title="Лучшее за 2024" posterUrl={`${P}/filmax-collection-101/380/570`} focused />
    <TvCollectionCard title="Скандинавский нуар" posterUrl={`${P}/filmax-collection-102/380/570`} />
    <TvCollectionCard title="Для всей семьи" posterUrl={`${P}/filmax-collection-103/380/570`} />
  </div>
);
