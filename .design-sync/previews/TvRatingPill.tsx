import { TvRatingPill } from "@filmax/tv-ui";

/** Пилюля поверх постера — на светлом и тёмном кадре. */
export const OnPosters = () => (
  <div style={{ display: "flex", gap: 18 }}>
    {[
      { rating: "8.3", url: "https://picsum.photos/seed/filmax-15/380/570" },
      { rating: "6.9", url: "https://picsum.photos/seed/filmax-16/380/570" },
    ].map((poster) => (
      <div key={poster.rating} style={{ position: "relative", width: 120, height: 180, borderRadius: 8, background: `#141414 url("${poster.url}") center/cover` }}>
        <div style={{ position: "absolute", top: 8, right: 8 }}>
          <TvRatingPill rating={poster.rating} />
        </div>
      </div>
    ))}
  </div>
);
