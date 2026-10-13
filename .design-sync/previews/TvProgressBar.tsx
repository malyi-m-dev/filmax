import { TvProgressBar } from "@filmax/tv-ui";

/** Полоса на кадре: начато, середина, почти досмотрено. */
export const Levels = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 18, width: 250 }}>
    {[0.1, 0.5, 0.92].map((progress) => (
      <div key={progress} style={{ background: "#2e2e2e", height: 40, display: "flex", alignItems: "flex-end", borderRadius: 10, overflow: "hidden" }}>
        <TvProgressBar progress={progress} />
      </div>
    ))}
  </div>
);
